package com.tarun.fieldpin.ui.map

import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.usecase.MAX_CHECK_IN_ACCURACY_M

data class MapUiState(
    val sites: List<Site> = emptyList(),
    val selectedSite: Site? = null,
    val userLocation: LatLng? = null,
    val locationState: LocationState = LocationState.NoPermission,
    val distanceToSelectedSiteM: Double? = null,
    val isUserInsideSelectedSite: Boolean? = null,
    val checkInAccuracyM: Double? = null,
    val isLoading: Boolean = true
) {
    val canCheckIn: Boolean get() = isUserInsideSelectedSite == true &&
        checkInAccuracyM?.let { it <= MAX_CHECK_IN_ACCURACY_M } == true
}
