package com.tarun.fieldpin.domain.usecase

import com.tarun.fieldpin.domain.model.LatLng
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInPolicyTest {
    private val boundary = listOf(
        LatLng(0.0, 0.0), LatLng(0.0, 0.01), LatLng(0.01, 0.01), LatLng(0.01, 0.0)
    )

    @Test fun insideWithAccurateFixIsAllowed() = assertTrue(canCheckIn(LatLng(0.005, 0.005), 10.0, boundary))
    @Test fun outsidePointIsBlocked() = assertFalse(canCheckIn(LatLng(0.02, 0.005), 10.0, boundary))
    @Test fun boundaryPointCountsAsInside() = assertTrue(canCheckIn(LatLng(0.0, 0.005), 10.0, boundary))
    @Test fun noLocationFixIsBlocked() = assertFalse(canCheckIn(null, null, boundary))
    @Test fun inaccurateFixIsBlocked() = assertFalse(canCheckIn(LatLng(0.005, 0.005), 50.1, boundary))
}
