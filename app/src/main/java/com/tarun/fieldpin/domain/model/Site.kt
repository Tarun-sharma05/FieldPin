package com.tarun.fieldpin.domain.model

data class Site (
    val id: String,
    val name: String,
    val category: String,
    val status: SiteStatus,
    val boundary: List<LatLng>,
    val areaSqm: Double,
    val perimeterM: Double,
    val lastInspectionAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val serverVersion: Long,
    val syncState: SyncState,
    val hasPendingSync: Boolean = false
)