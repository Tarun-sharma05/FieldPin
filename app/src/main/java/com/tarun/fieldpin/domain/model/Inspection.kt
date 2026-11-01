package com.tarun.fieldpin.domain.model

data class Inspection (
    val id: String,
    val siteId: String,
    val checkInAt: Long,
    val checkInLat: Double,
    val checkInLng: Double,
    val checkInAccuracyM: Double,
    val condition: Condition,
    val notes: String,
    val status: InspectionStatus,
    val createdAt: Long,
    val syncState: SyncState
)