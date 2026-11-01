package com.tarun.fieldpin.domain.model

data class InspectionWithPhotos (
    val inspection: Inspection,
    val photos: List<Photo>
)