package com.tarun.fieldpin.data.remote

import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FakeSiteRemoteDataSourceTest {
    private val site = Site(
        id = "site-1", name = "Park", category = "Park", status = SiteStatus.ACTIVE,
        boundary = listOf(LatLng(26.9, 75.8), LatLng(26.9, 75.81), LatLng(26.91, 75.8)),
        areaSqm = 10.0, perimeterM = 12.0, lastInspectionAt = null,
        createdAt = 1L, updatedAt = 1L, serverVersion = 0L, syncState = SyncState.PENDING
    )

    @Test fun acceptedWriteIncrementsVersionAndStaleWriteConflicts() = runBlocking {
        val remote = FakeSiteRemoteDataSource(0, 0f, Random(1))
        val accepted = remote.push(site) as SitePushResult.Accepted
        assertEquals(1L, accepted.site.serverVersion)
        val stale = remote.push(site) as SitePushResult.Conflict
        assertEquals(1L, stale.serverSite.serverVersion)
    }

    @Test fun configuredFailureReturnsRetryableReason() = runBlocking {
        val result = FakeSiteRemoteDataSource(0, 1f).push(site)
        assertTrue(result is SitePushResult.Failed)
        assertTrue((result as SitePushResult.Failed).reason.isNotBlank())
    }
}
