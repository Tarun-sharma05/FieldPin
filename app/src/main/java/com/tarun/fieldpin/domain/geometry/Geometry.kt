package com.tarun.fieldpin.domain.geometry

import com.tarun.fieldpin.domain.model.LatLng
import kotlin.math.abs
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


/**
 * Returns the perimeter of the polygon in metres.
 * Sums the great-circle distance of each edge, including the closing edge
 * from the last point back to the first.
 * Returns 0.0 for fewer than 3 points (not a valid polygon).
 **/
fun polygonPerimeterM(points: List<LatLng>): Double {
    if (points.size < 3) return 0.0
    var total = 0.0
    for(i in points.indices) {
        // % points.size wraps the last index back to 0, closing the polygon
        val next = (i + 1) % points.size
        total += distanceMeters(points[i], points[next])
    }
    return total
}

/**
 * Returns the area of the polygon in square metres using the
 * Chamberlain & Duquette spherical excess algorithm.
 *
 * Why this formula:
 * Treating lat/lng as flat x/y distorts area at higher latitudes becuase
 * longitude lines converge near the poles. This algorithm integrates
 * spherical trapezoids along each edge, giving the true surface area.
 *
 * abs() makes the result the same whether points are clockwise or counter-clockwise.
 *
 * Returns 0.0 for fewer than 3 points.
 */
fun polygonAreaSqm(points: List<LatLng>): Double {
    if(points.size < 3) return 0.0
    var sum = 0.0
    for (i in points.indices){
        val next = (i + 1) % points.size
        val deltaLonRad = Math.toRadians(points[next].longitude - points[i].longitude)
        val latIRad = Math.toRadians(points[i].latitude)
        val latJRad = Math.toRadians(points[next].latitude)
        // Each term is the signed area of a spherical trapezoid between
        // this edge and the equator. Summing and taking abs() gives the polygon area.
        sum += deltaLonRad * (2 + sin(latIRad) + sin(latJRad))
    }
    return abs(sum) * EARTH_RADIUS_M * EARTH_RADIUS_M / 2.0
}

private const val BOUNDARY_TOLERANCE_M = 0.1
/**
 * Calculates the shortest distance in metres from a point P to a line segment AB.
 *
 * Method:
 * Projects P onto segment AB using local equirectangular coordinates (scaling longitude
 * by cos(latitude) to account for meridian convergence). Clamps parameter t to [0, 1]
 * to bound the closest point within segment AB, then computes exact Haversine distance.
 */
fun distanceToSegmentMeters(p: LatLng, a: LatLng, b: LatLng): Double {
    val latRad = Math.toRadians(p.latitude)
    val cosLat = cos(latRad)

    // Vector AB in local projected coordinates
    val dx = Math.toRadians(b.longitude - a.longitude) * cosLat
    val  dy = Math.toRadians(b.latitude - a.latitude)

    // Vector AP in local projected coordintes
    val px = Math.toRadians(p.longitude - a.longitude) * cosLat
    val py = Math.toRadians(p.latitude - a.latitude)

    val segmentLengthSq = dx * dx + dy * dy
    if(segmentLengthSq == 0.0) {
        // Segment AB is a single point (A == B)
        return distanceMeters(p, a)
    }


    // Projection scalar t = (AP · AB) / |AB|²
    // Clamped to [0.0, 1.0] so the closest point stays on segment AB
    val t = ((px * dx + py * dy) / segmentLengthSq).coerceIn(0.0, 1.0)

    // Interpolate clossest point coordinates C on segment AB
    val closestLat = a.latitude + t * (b.latitude - a.latitude)
    val closestLon = a.longitude + t * (b.longitude - a.longitude)

    return  distanceMeters(p, LatLng(closestLat, closestLon))
}


/**
 * Determines whether a given point lies inside or on the boundary of a polygon.
 *
 * Algorithm:
 * 1. Explicit Boundary Check: If the point is within 0.1 m of any segment or vertex,
 *    return true (satisfies Spec S1 deterministic boundary rule).
 * 2. Ray-Casting (Even-Odd Rule): Casts a ray due East (increasing longitude) at constant
 *    latitude. Toggles inside status every time the ray crosses a polygon edge.
 *
 * Returns false for invalid polygons (< 3 points).
 */
fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
    if (polygon.size < 3) return false

    // 1. Boundary / edge check
    for (i in polygon.indices) {
        val next = (i + 1) % polygon.size
        if (distanceToSegmentMeters(point, polygon[i], polygon[next]) <= BOUNDARY_TOLERANCE_M) {
            return true
        }
    }

    // 2. Ray-casting algorithm
    var inside = false
    for (i in polygon.indices) {
        val j = (i + 1) % polygon.size
        val a = polygon[i]
        val b = polygon[j]

        // Check if edge AB straddles the point's latitude
        val straddlesLat = (a.latitude > point.latitude) != (b.latitude > point.latitude)
        if(straddlesLat){
            // Compute exact longitude where edge AB crosses point.latitude
            val intersectLon = (b.longitude - a.longitude) * (point.latitude - a.latitude) / (b.latitude - a.latitude) + a.longitude
            if (point.longitude < intersectLon){
                inside = !inside
            }
        }
    }
    return inside
}

/**
 * Returns the shortest distance in metres from a point to a polygon.
 *
 * Rules:
 * - If the point is inside or on the boundary of the polygon, returns 0.0.
 * - If outside, returns the geodesic distance to the nearest polygon edge.
 * - Returns 0.0 for invalid polygons (< 3 points).
 */
fun distanceToPolygonMeters(point: LatLng, polygon: List<LatLng>): Double {
    if ( polygon.size < 3) return 0.0
    if(isPointInPolygon(point, polygon)) return 0.0

    var minDistance = Double.MAX_VALUE
    for (i in polygon.indices) {
        val next = (i + 1) % polygon.size
        val dist = distanceToSegmentMeters(point, polygon[i], polygon[next])
        if(dist < minDistance){
            minDistance = dist
        }
    }
    return minDistance
}