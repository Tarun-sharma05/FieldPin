package com.tarun.fieldpin.domain.model

data class Photo (
    val id: String,
    val inspectionId: String,
    val filePath: String,
    val takenAt: Long,
    val lat: Double,
    val lng: Double,
    val accuracyM: Double
)