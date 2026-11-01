package com.tarun.fieldpin.data.local

import com.tarun.fieldpin.data.local.converter.Converters
import com.tarun.fieldpin.data.local.entity.SiteEntity
import com.tarun.fieldpin.data.local.entity.SiteWithPendingCount
import com.tarun.fieldpin.data.local.mapper.toDomain
import com.tarun.fieldpin.data.local.mapper.toEntity
import com.tarun.fieldpin.domain.model.Condition
import com.tarun.fieldpin.domain.model.Inspection
import com.tarun.fieldpin.domain.model.InspectionStatus
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.Photo
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MapperUnitTest {

    private val converters = Converters()

    @Test
    fun boundaryConverter_roundTrip_preservesLatLngList() {
        val original = listOf(
            LatLng(18.5204, 73.8567),
            LatLng(18.5215, 73.8578),
            LatLng(18.5225, 73.8589)
        )
        val json = converters.fromLatLngList(original)
        val restored = converters.toLatLngList(json)

        assertEquals(original, restored)
    }

    @Test
    fun siteEntityToDomain_mapsFieldsCorrectly() {
        val entity = SiteEntity(
            id = "site-101",
            name = "Test Park",
            category = "Park",
            status = "ACTIVE",
            boundaryJson = converters.fromLatLngList(listOf(LatLng(10.0, 10.0), LatLng(10.0, 20.0), LatLng(20.0, 10.0))),
            areaSqm = 500.0,
            perimeterM = 100.0,
            lastInspectionAt = 1000L,
            createdAt = 500L,
            updatedAt = 1200L,
            serverVersion = 2L,
            syncState = "SYNCED"
        )
        val siteWithCount = SiteWithPendingCount(site = entity, pendingInspectionsCount = 0)

        val domain = siteWithCount.toDomain()

        assertEquals("site-101", domain.id)
        assertEquals("Test Park", domain.name)
        assertEquals(SiteStatus.ACTIVE, domain.status)
        assertEquals(SyncState.SYNCED, domain.syncState)
        assertFalse(domain.hasPendingSync)
    }

    @Test
    fun siteDomainToEntity_mapsFieldsCorrectly() {
        val domain = Site(
            id = "site-202",
            name = "Water Plant",
            category = "Utility",
            status = SiteStatus.NEEDS_REPAIR,
            boundary = listOf(LatLng(18.0, 73.0), LatLng(18.1, 73.0), LatLng(18.0, 73.1)),
            areaSqm = 1200.0,
            perimeterM = 250.0,
            lastInspectionAt = null,
            createdAt = 100L,
            updatedAt = 200L,
            serverVersion = 1L,
            syncState = SyncState.PENDING,
            hasPendingSync = true
        )

        val entity = domain.toEntity()

        assertEquals("site-202", entity.id)
        assertEquals("NEEDS_REPAIR", entity.status)
        assertEquals("PENDING", entity.syncState)
    }

    @Test
    fun inspectionEntityAndDomain_roundTrip() {
        val domain = Inspection(
            id = "insp-1",
            siteId = "site-1",
            checkInAt = 1000L,
            checkInLat = 18.5204,
            checkInLng = 73.8567,
            checkInAccuracyM = 5.0,
            condition = Condition.GOOD,
            notes = "All intact",
            status = InspectionStatus.SUBMITTED,
            createdAt = 900L,
            syncState = SyncState.SYNCED
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()

        assertEquals(domain, restored)
    }

    @Test
    fun photoEntityAndDomain_roundTrip() {
        val domain = Photo(
            id = "photo-1",
            inspectionId = "insp-1",
            filePath = "/path/to/img.jpg",
            takenAt = 1000L,
            lat = 18.52,
            lng = 73.85,
            accuracyM = 3.0
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()

        assertEquals(domain, restored)
    }
}
