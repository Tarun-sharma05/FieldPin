package com.tarun.fieldpin.domain.model

import com.tarun.fieldpin.data.local.entity.SiteEntity
import com.tarun.fieldpin.data.local.entity.SiteWithPendingCount
import com.tarun.fieldpin.data.local.mapper.toDomain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingSyncUnitTest {

    @Test
    fun hasPendingSync_returnsFalse_whenSiteAndInspectionsAreSynced() {
        val entity = createSiteEntity(syncState = "SYNCED")
        val siteWithCount = SiteWithPendingCount(site = entity, pendingInspectionsCount = 0)

        val domainSite = siteWithCount.toDomain()

        assertFalse(domainSite.hasPendingSync)
    }

    @Test
    fun hasPendingSync_returnsTrue_whenSiteIsPending() {
        val entity = createSiteEntity(syncState = "PENDING")
        val siteWithCount = SiteWithPendingCount(site = entity, pendingInspectionsCount = 0)

        val domainSite = siteWithCount.toDomain()

        assertTrue(domainSite.hasPendingSync)
    }

    @Test
    fun hasPendingSync_returnsTrue_whenSiteIsFailed() {
        val entity = createSiteEntity(syncState = "FAILED")
        val siteWithCount = SiteWithPendingCount(site = entity, pendingInspectionsCount = 0)

        val domainSite = siteWithCount.toDomain()

        assertTrue(domainSite.hasPendingSync)
    }

    @Test
    fun hasPendingSync_returnsTrue_whenSiteIsSyncedButHasPendingInspections() {
        val entity = createSiteEntity(syncState = "SYNCED")
        val siteWithCount = SiteWithPendingCount(site = entity, pendingInspectionsCount = 2)

        val domainSite = siteWithCount.toDomain()

        assertTrue(domainSite.hasPendingSync)
    }

    private fun createSiteEntity(syncState: String): SiteEntity {
        return SiteEntity(
            id = "site-1",
            name = "Test Site",
            category = "Park",
            status = "ACTIVE",
            boundaryJson = "[]",
            areaSqm = 100.0,
            perimeterM = 40.0,
            lastInspectionAt = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            serverVersion = 1L,
            syncState = syncState
        )
    }
}
