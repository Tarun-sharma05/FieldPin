package com.tarun.fieldpin.domain.usecase

import com.tarun.fieldpin.domain.geometry.isPointInPolygon
import com.tarun.fieldpin.domain.model.LatLng

const val MAX_CHECK_IN_ACCURACY_M = 50.0

fun canCheckIn(point: LatLng?, accuracyM: Double?, boundary: List<LatLng>): Boolean =
    point != null && accuracyM != null && accuracyM <= MAX_CHECK_IN_ACCURACY_M &&
        isPointInPolygon(point, boundary)
