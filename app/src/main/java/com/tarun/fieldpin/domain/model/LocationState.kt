package com.tarun.fieldpin.domain.model

sealed interface LocationState {
    data class Fix(
        val lat: Double,
        val lng: Double,
        val accuracyM: Float
    ) : LocationState

    data object NoPermission : LocationState
    data object GpsOff : LocationState
    data object Unavailable : LocationState
}
