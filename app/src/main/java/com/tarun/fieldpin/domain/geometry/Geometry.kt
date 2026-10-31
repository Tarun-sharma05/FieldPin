package com.tarun.fieldpin.domain.geometry

import com.tarun.fieldpin.domain.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


private const val EARTH_RADIUS_M = 6_371_008.8

private const val SQM_PER_ACRE = 4_046.8564224


/**
 * Returns the great-circle distance in metres between two points using the
 * Haversine formula. Haversine stays numerically stable at short distances
 * where the spherical law of cosines loses precision due to floating-point
 * cancellation.
 */
fun distanceMeters(a: LatLng, b: LatLng): Double {
    val lat1Rad = Math.toRadians(a.latitude)
    val lat2Rad = Math.toRadians(b.latitude)
    val deltaLatRad = Math.toRadians(b.latitude - a.latitude)
    val deltaLonRad = Math.toRadians(b.longitude - a.longitude)

    val sinHalfDeltaLat = sin(deltaLatRad / 2)
    val sinHalfDeltaLon = sin(deltaLonRad / 2)

    // h is the haversine of the central angle.
    // It combines the north-south gap (deltaLat) and east-west gap (deltaLon),
    // scaled by cos(lat) because longitude lines get closer together near the poles.
    val h = sinHalfDeltaLat * sinHalfDeltaLat + cos(lat1Rad) * cos(lat2Rad) * sinHalfDeltaLon * sinHalfDeltaLon

    // atan2 is the numerically safe way to get the central angle from h.
    // We use atan2(√h, √(1-h)) instead of asin(√h) to avoid NaN near antipodal points.
    val centralAngle = 2.0 * atan2(sqrt(h), sqrt(1.0 - h))

    return EARTH_RADIUS_M * centralAngle
}

/**
 * Converts square metres to acres.
 * 1 acre = 4046.8564224 m² (international definition).
 */
fun sqmToAcres(sqm: Double): Double = sqm / SQM_PER_ACRE