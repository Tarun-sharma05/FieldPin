package com.tarun.fieldpin.data.local.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedSiteDto(
    val id: String,
    val name: String,
    val category: String,
    val status: String,
    val boundary: List<SeedLatLngDto>
)

@Serializable
data class SeedLatLngDto(
    val latitude: Double,
    val longitude: Double
)
