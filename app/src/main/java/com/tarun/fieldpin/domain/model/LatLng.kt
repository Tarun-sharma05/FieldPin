package com.tarun.fieldpin.domain.model

data class LatLng(
    val latitude: Double,
    val longitude: Double
){

    init {
        require(latitude in -90.0..90.0){
            "Latitude must be in [[-90, 90], got $latitude"
        }
        require(longitude in -180.0..180.0){
            "Longitude must be in [-180, 180], got $longitude"
        }
    }
}