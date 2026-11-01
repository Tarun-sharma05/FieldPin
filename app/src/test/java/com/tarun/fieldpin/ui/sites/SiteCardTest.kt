package com.tarun.fieldpin.ui.sites

import com.tarun.fieldpin.ui.components.formatLastInspectionDate
import com.tarun.fieldpin.ui.components.formatSiteArea
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SiteCardTest {

    @Test
    fun formatSiteArea_belowTenThousand_showsSquareMeters() {
        val result = formatSiteArea(8450.0)
        assertEquals("8,450 m²", result)
    }

    @Test
    fun formatSiteArea_atOrAboveTenThousand_showsAcres() {
        val result = formatSiteArea(10000.0)
        assertEquals("2.47 acres", result)
    }

    @Test
    fun formatLastInspectionDate_null_returnsNotInspected() {
        val result = formatLastInspectionDate(null)
        assertEquals("Not inspected", result)
    }

    @Test
    fun formatLastInspectionDate_validEpoch_returnsFormattedDate() {
        val result = formatLastInspectionDate(1792800000000L)
        assertTrue(result.contains("2026"))
    }
}
