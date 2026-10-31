package com.tarun.fieldpin.domain.geometry

import com.tarun.fieldpin.domain.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryUnitTest {

    // 100m x 100m square in Pune, India (18.5204 N, 73.8567 E)
    // 1 deg lat ≈ 111,000 m -> 100 m ≈ 0.0009009 deg
    // 1 deg lon at 18.5204° N ≈ 105,250 m -> 100 m ≈ 0.0009501 deg
    private val p1 = LatLng(18.5204, 73.8567)
    private val p2 = LatLng(18.5204, 73.8567 + 0.0009501)
    private val p3 = LatLng(18.5204 + 0.0009009, 73.8567 + 0.0009501)
    private val p4 = LatLng(18.5204 + 0.0009009, 73.8567)

    private val squarePolygon = listOf(p1, p2, p3, p4)

    @Test
    fun `square area is approximately 10000 sqm within 1 percent`() {
        val area = polygonAreaSqm(squarePolygon)
        // 10,000 m² ± 1% = 9,900 to 10,100
        assertEquals(10_000.0, area, 100.0)
    }

    @Test
    fun `square perimeter is approximately 400 m`() {
        val perimeter = polygonPerimeterM(squarePolygon)
        assertEquals(400.0, perimeter, 4.0)
    }

    @Test
    fun `direction invariance - area is identical for CW and CCW point orders`() {
        val areaCW = polygonAreaSqm(squarePolygon)
        val areaCCW = polygonAreaSqm(squarePolygon.reversed())
        assertEquals(areaCW, areaCCW, 1e-6)
    }

    @Test
    fun `direction invariance - perimeter is identical for CW and CCW point orders`() {
        val perimCW = polygonPerimeterM(squarePolygon)
        val perimCCW = polygonPerimeterM(squarePolygon.reversed())
        assertEquals(perimCW, perimCCW, 1e-6)
    }

    @Test
    fun `point inside polygon`() {
        val centerPoint = LatLng(18.5204 + 0.00045, 73.8567 + 0.00047)
        assertTrue(isPointInPolygon(centerPoint, squarePolygon))
        assertEquals(0.0, distanceToPolygonMeters(centerPoint, squarePolygon), 1e-6)
    }

    @Test
    fun `point outside polygon`() {
        val outsidePoint = LatLng(18.5204 - 0.001, 73.8567)
        assertFalse(isPointInPolygon(outsidePoint, squarePolygon))

        val distance = distanceToPolygonMeters(outsidePoint, squarePolygon)
        assertTrue("Distance should be positive", distance > 0.0)
        // Point is ~0.001 deg lat (approx 111 m) south of p1
        assertEquals(111.0, distance, 5.0)
    }

    @Test
    fun `point on vertex is inside`() {
        assertTrue(isPointInPolygon(p1, squarePolygon))
        assertEquals(0.0, distanceToPolygonMeters(p1, squarePolygon), 1e-6)
    }

    @Test
    fun `point on edge is inside`() {
        val midpointOnEdge = LatLng(p1.latitude, p1.longitude + 0.000475)
        assertTrue(isPointInPolygon(midpointOnEdge, squarePolygon))
        assertEquals(0.0, distanceToPolygonMeters(midpointOnEdge, squarePolygon), 1e-6)
    }

    @Test
    fun `concave polygon notch test`() {
        // L-shaped polygon
        val lShape = listOf(
            LatLng(18.520, 73.850),
            LatLng(18.520, 73.852),
            LatLng(18.522, 73.852),
            LatLng(18.522, 73.851),
            LatLng(18.521, 73.851),
            LatLng(18.521, 73.850)
        )

        val insidePoint = LatLng(18.5205, 73.8505)
        val pointInNotch = LatLng(18.5215, 73.8505)

        assertTrue("Point in main body should be inside", isPointInPolygon(insidePoint, lShape))
        assertFalse("Point in notch should be outside", isPointInPolygon(pointInNotch, lShape))
    }

    @Test
    fun `triangle polygon area and perimeter`() {
        val triangle = listOf(
            LatLng(18.520, 73.850),
            LatLng(18.520, 73.851),
            LatLng(18.521, 73.850)
        )
        val area = polygonAreaSqm(triangle)
        val perimeter = polygonPerimeterM(triangle)

        assertTrue(area > 0.0)
        assertTrue(perimeter > 0.0)
    }

    @Test
    fun `invalid polygon with less than 3 points returns safe defaults`() {
        val emptyPolygon = emptyList<LatLng>()
        val twoPointPolygon = listOf(LatLng(18.520, 73.850), LatLng(18.521, 73.851))

        assertEquals(0.0, polygonAreaSqm(emptyPolygon), 1e-6)
        assertEquals(0.0, polygonAreaSqm(twoPointPolygon), 1e-6)

        assertEquals(0.0, polygonPerimeterM(emptyPolygon), 1e-6)
        assertEquals(0.0, polygonPerimeterM(twoPointPolygon), 1e-6)

        assertFalse(isPointInPolygon(p1, emptyPolygon))
        assertFalse(isPointInPolygon(p1, twoPointPolygon))

        assertEquals(0.0, distanceToPolygonMeters(p1, emptyPolygon), 1e-6)
        assertEquals(0.0, distanceToPolygonMeters(p1, twoPointPolygon), 1e-6)
    }

    @Test
    fun `sqmToAcres conversion`() {
        val sqmInOneAcre = 4_046.8564224
        assertEquals(1.0, sqmToAcres(sqmInOneAcre), 1e-6)
        assertEquals(2.5, sqmToAcres(sqmInOneAcre * 2.5), 1e-6)
        assertEquals(0.0, sqmToAcres(0.0), 1e-6)
    }

    @Test
    fun `distanceMeters between identical points is 0`() {
        assertEquals(0.0, distanceMeters(p1, p1), 1e-6)
    }
}
