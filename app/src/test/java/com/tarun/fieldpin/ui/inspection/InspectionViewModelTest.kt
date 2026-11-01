package com.tarun.fieldpin.ui.inspection

import com.tarun.fieldpin.domain.model.Condition
import com.tarun.fieldpin.domain.model.Inspection
import com.tarun.fieldpin.domain.model.InspectionWithPhotos
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.model.Photo
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.repository.InspectionRepository
import com.tarun.fieldpin.domain.repository.LocationProvider
import com.tarun.fieldpin.domain.repository.SiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InspectionViewModelTest {
    @After fun resetMainDispatcher() = Dispatchers.resetMain()

    @Test
    fun checkInCapturesFixAndSubmitSavesPendingInspection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val site = testSite()
        val location = TestLocationProvider(LocationState.Fix(0.005, 0.005, 7f))
        val inspections = TestInspectionRepository()
        val viewModel = InspectionViewModel("site-1", TestSiteRepository(site), inspections, location)
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canStart)
        viewModel.startInspection()
        viewModel.onConditionChanged(Condition.POOR)
        viewModel.onNotesChanged("Fence is damaged")
        viewModel.submit()
        advanceUntilIdle()

        val saved = inspections.saved.single()
        assertEquals(site.id, saved.siteId)
        assertEquals(0.005, saved.checkInLat)
        assertEquals(0.005, saved.checkInLng)
        assertEquals(7.0, saved.checkInAccuracyM)
        assertEquals(Condition.POOR, saved.condition)
        assertEquals("Fence is damaged", saved.notes)
        assertEquals(SyncState.PENDING, saved.syncState)
        assertTrue(viewModel.uiState.value.isSubmitted)
        collection.cancel()
    }
}

private fun testSite() = Site(
    id = "site-1", name = "Park", category = "Park", status = SiteStatus.ACTIVE,
    boundary = listOf(LatLng(0.0, 0.0), LatLng(0.0, 0.01), LatLng(0.01, 0.01), LatLng(0.01, 0.0)),
    areaSqm = 0.0, perimeterM = 0.0, lastInspectionAt = null, createdAt = 0, updatedAt = 0,
    serverVersion = 0, syncState = SyncState.SYNCED
)

private class TestLocationProvider(initial: LocationState) : LocationProvider {
    private val flow = MutableStateFlow(initial)
    override fun locationUpdates(): Flow<LocationState> = flow
}

private class TestSiteRepository(private val site: Site) : SiteRepository {
    override fun observeSites(query: String?, status: SiteStatus?): Flow<List<Site>> = MutableStateFlow(listOf(site))
    override fun getSiteById(id: String): Flow<Site?> = MutableStateFlow(site.takeIf { it.id == id })
    override suspend fun saveSite(site: Site) = Unit
    override suspend fun seedDatabaseIfEmpty() = Unit
}

private class TestInspectionRepository : InspectionRepository {
    val saved = mutableListOf<Inspection>()
    override fun observeInspectionForSite(siteId: String): Flow<List<InspectionWithPhotos>> = MutableStateFlow(emptyList())
    override suspend fun saveInspection(inspection: Inspection, photos: List<Photo>) { saved += inspection }
}
