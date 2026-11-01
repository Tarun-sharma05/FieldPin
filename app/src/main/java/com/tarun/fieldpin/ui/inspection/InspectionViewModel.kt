package com.tarun.fieldpin.ui.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarun.fieldpin.domain.geometry.distanceToPolygonMeters
import com.tarun.fieldpin.domain.geometry.isPointInPolygon
import com.tarun.fieldpin.domain.model.Condition
import com.tarun.fieldpin.domain.model.Inspection
import com.tarun.fieldpin.domain.model.InspectionStatus
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.model.Photo
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.repository.InspectionRepository
import com.tarun.fieldpin.domain.repository.LocationProvider
import com.tarun.fieldpin.domain.repository.SiteRepository
import com.tarun.fieldpin.domain.usecase.canCheckIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class InspectionViewModel(
    siteId: String,
    siteRepository: SiteRepository,
    private val inspectionRepository: InspectionRepository,
    locationProvider: LocationProvider
) : ViewModel() {
    private val formState = MutableStateFlow(InspectionUiState())

    val uiState: StateFlow<InspectionUiState> = combine(
        siteRepository.getSiteById(siteId),
        locationProvider.locationUpdates(),
        formState
    ) { site, locationState, form ->
        val fix = locationState as? LocationState.Fix
        val point = fix?.let { LatLng(it.lat, it.lng) }
        val inside = if (site != null && point != null) isPointInPolygon(point, site.boundary) else null
        form.copy(
            site = site,
            locationState = locationState,
            distanceToSiteM = if (site != null && point != null) distanceToPolygonMeters(point, site.boundary) else null,
            isInsideSite = inside,
            canStart = site != null && canCheckIn(point, fix?.accuracyM?.toDouble(), site.boundary),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0L), InspectionUiState())

    fun startInspection() {
        val state = uiState.value
        val fix = state.locationState as? LocationState.Fix ?: return
        if (!state.canStart || state.isStarted) return
        formState.update {
            it.copy(
                isStarted = true,
                inspectionId = UUID.randomUUID().toString(),
                checkInAt = System.currentTimeMillis(),
                checkInLat = fix.lat,
                checkInLng = fix.lng,
                checkInAccuracyM = fix.accuracyM.toDouble(),
                errorMessage = null
            )
        }
    }

    fun onConditionChanged(condition: Condition) {
        formState.update { it.copy(condition = condition) }
    }

    fun onNotesChanged(notes: String) {
        formState.update { it.copy(notes = notes) }
    }

    fun onPhotoCaptured(filePath: String, takenAt: Long, lat: Double, lng: Double, accuracyM: Double) {
        val inspectionId = formState.value.inspectionId ?: return
        formState.update { state ->
            state.copy(photos = state.photos + Photo(
                id = UUID.randomUUID().toString(),
                inspectionId = inspectionId,
                filePath = filePath,
                takenAt = takenAt,
                lat = lat,
                lng = lng,
                accuracyM = accuracyM
            ))
        }
    }

    fun submit() {
        val site = uiState.value.site ?: return
        val form = formState.value
        val checkInAt = form.checkInAt ?: return
        val checkInLat = form.checkInLat ?: return
        val checkInLng = form.checkInLng ?: return
        val accuracy = form.checkInAccuracyM ?: return
        if (!form.isStarted || form.isSubmitting || form.isSubmitted) return
        formState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val inspection = Inspection(
                id = UUID.randomUUID().toString(),
                siteId = site.id,
                checkInAt = checkInAt,
                checkInLat = checkInLat,
                checkInLng = checkInLng,
                checkInAccuracyM = accuracy,
                condition = form.condition,
                notes = form.notes,
                status = InspectionStatus.SUBMITTED,
                createdAt = checkInAt,
                syncState = SyncState.PENDING
            )
            try {
                inspectionRepository.saveInspection(inspection, form.photos)
                formState.update { it.copy(isSubmitting = false, isSubmitted = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                formState.update {
                    it.copy(isSubmitting = false, errorMessage = error.message ?: "Could not submit inspection.")
                }
            }
        }
    }
}
