package com.tarun.fieldpin.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarun.fieldpin.domain.geometry.distanceToPolygonMeters
import com.tarun.fieldpin.domain.geometry.isPointInPolygon
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.repository.LocationProvider
import com.tarun.fieldpin.domain.repository.SiteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    siteRepository: SiteRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {
    private val selectedSiteId = MutableStateFlow<String?>(null)
    private val locationEnabled = MutableStateFlow(false)
    private val locationStates = locationEnabled.flatMapLatest { enabled ->
        if (enabled) locationProvider.locationUpdates() else flowOf(LocationState.NoPermission)
    }

    val uiState: StateFlow<MapUiState> = combine(
        siteRepository.observeSites(),
        selectedSiteId,
        locationStates
    ) { sites, selectedId, locationState ->
        val selectedSite = sites.firstOrNull { it.id == selectedId }
        val fix = locationState as? LocationState.Fix
        val userLocation = fix?.let { LatLng(it.lat, it.lng) }
        val inside = if (selectedSite != null && userLocation != null) {
            isPointInPolygon(userLocation, selectedSite.boundary)
        } else null
        MapUiState(
            sites = sites,
            selectedSite = selectedSite,
            userLocation = userLocation,
            locationState = locationState,
            distanceToSelectedSiteM = if (selectedSite != null && userLocation != null) {
                distanceToPolygonMeters(userLocation, selectedSite.boundary)
            } else null,
            isUserInsideSelectedSite = inside,
            checkInAccuracyM = fix?.accuracyM?.toDouble(),
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(0L),
        MapUiState()
    )

    fun onSiteTap(siteId: String) {
        selectedSiteId.value = siteId.takeIf(String::isNotEmpty)
    }

    fun setLocationTrackingEnabled(enabled: Boolean) {
        locationEnabled.value = enabled
    }
}
