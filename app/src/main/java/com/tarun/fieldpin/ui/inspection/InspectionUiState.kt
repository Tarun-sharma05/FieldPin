package com.tarun.fieldpin.ui.inspection

import com.tarun.fieldpin.domain.model.Condition
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.model.Photo
import com.tarun.fieldpin.domain.model.Site

data class InspectionUiState(
    val site: Site? = null,
    val locationState: LocationState = LocationState.NoPermission,
    val distanceToSiteM: Double? = null,
    val isInsideSite: Boolean? = null,
    val canStart: Boolean = false,
    val isStarted: Boolean = false,
    val inspectionId: String? = null,
    val checkInAt: Long? = null,
    val checkInLat: Double? = null,
    val checkInLng: Double? = null,
    val checkInAccuracyM: Double? = null,
    val condition: Condition = Condition.GOOD,
    val notes: String = "",
    val photos: List<Photo> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
