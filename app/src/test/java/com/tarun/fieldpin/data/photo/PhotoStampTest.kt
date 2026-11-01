package com.tarun.fieldpin.data.photo

import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoStampTest {
    @Test
    fun stampTextContainsSiteCoordinatesAccuracyAndTime() {
        val lines = formatPhotoStamp(PhotoStamp("Jaipur Park", 26.9124, 75.7873, 4.5, 0L), Locale.US)

        assertEquals("Jaipur Park", lines[0])
        assertEquals("26.912400, 75.787300", lines[1])
        assertEquals("Accuracy ±4.5 m", lines[2])
        assertTrue(lines[3].startsWith("1970-01-01"))
    }
}
