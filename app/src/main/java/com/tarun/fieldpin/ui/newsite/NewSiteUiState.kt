package com.tarun.fieldpin.ui.newsite

import com.tarun.fieldpin.domain.model.LatLng

data class NewSiteUiState(
    val name: String = "New site",
    val category: String = "Park",
    val boundary: List<LatLng> = emptyList(),
    val areaSqm: Double = 0.0,
    val areaAcres: Double = 0.0,
    val perimeterM: Double = 0.0,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean get() = boundary.size >= 3 && name.isNotBlank() && category.isNotBlank() && !isSaving
}
