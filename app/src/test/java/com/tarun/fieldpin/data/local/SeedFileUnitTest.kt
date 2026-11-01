package com.tarun.fieldpin.data.local

import com.tarun.fieldpin.data.local.seed.SeedSiteDto
import com.tarun.fieldpin.domain.geometry.polygonAreaSqm
import com.tarun.fieldpin.domain.geometry.polygonPerimeterM
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.SiteStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedFileUnitTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun seedJson_parsesSuccessfullyAndValidatesPolygonGeometry() {
        val seedFile = File("src/main/assets/seed_sites.json")
        assertTrue("seed_sites.json must exist in assets", seedFile.exists())

        val jsonText = seedFile.readText()
        val sites = json.decodeFromString<List<SeedSiteDto>>(jsonText)

        assertTrue("Seed file must contain at least 1 site", sites.isNotEmpty())

        for (site in sites) {
            assertTrue("Site id must not be empty", site.id.isNotBlank())
            assertTrue("Site name must not be empty", site.name.isNotBlank())
            assertTrue(
                "Site boundary must have at least 3 points, got ${site.boundary.size}",
                site.boundary.size >= 3
            )

            // Validate status enum
            val status = SiteStatus.valueOf(site.status)
            assertNotNull(status)

            // Compute S1 geometry metrics
            val domainBoundary = site.boundary.map { LatLng(it.latitude, it.longitude) }
            val area = polygonAreaSqm(domainBoundary)
            val perimeter = polygonPerimeterM(domainBoundary)

            assertTrue("Area for site ${site.name} must be > 0", area > 0.0)
            assertTrue("Perimeter for site ${site.name} must be > 0", perimeter > 0.0)
        }
    }
}
