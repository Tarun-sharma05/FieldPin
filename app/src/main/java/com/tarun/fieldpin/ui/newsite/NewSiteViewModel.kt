package com.tarun.fieldpin.ui.newsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarun.fieldpin.domain.geometry.polygonAreaSqm
import com.tarun.fieldpin.domain.geometry.polygonPerimeterM
import com.tarun.fieldpin.domain.geometry.sqmToAcres
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.repository.SiteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class NewSiteViewModel(private val siteRepository: SiteRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NewSiteUiState())
    val uiState: StateFlow<NewSiteUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onCategoryChanged(category: String) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onMapTap(point: LatLng) {
        _uiState.update { state -> state.withBoundary(state.boundary + point) }
    }

    fun undoLastCorner() {
        _uiState.update { state ->
            if (state.boundary.isEmpty()) state
            else state.withBoundary(state.boundary.dropLast(1))
        }
    }

    fun saveSite() {
        val state = _uiState.value
        if (!state.canSave) return
        val now = System.currentTimeMillis()
        val site = Site(
            id = UUID.randomUUID().toString(),
            name = state.name.trim(),
            category = state.category.trim(),
            status = SiteStatus.ACTIVE,
            boundary = state.boundary,
            areaSqm = state.areaSqm,
            perimeterM = state.perimeterM,
            lastInspectionAt = null,
            createdAt = now,
            updatedAt = now,
            serverVersion = 0L,
            syncState = SyncState.PENDING
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                siteRepository.saveSite(site)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = error.message ?: "Could not save this site.")
                }
            }
        }
    }

    private fun NewSiteUiState.withBoundary(points: List<LatLng>): NewSiteUiState {
        val area = polygonAreaSqm(points)
        return copy(
            boundary = points,
            areaSqm = area,
            areaAcres = sqmToAcres(area),
            perimeterM = polygonPerimeterM(points),
            errorMessage = null
        )
    }
}
