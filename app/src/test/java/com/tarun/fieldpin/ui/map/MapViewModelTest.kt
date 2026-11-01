package com.tarun.fieldpin.ui.map

import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.LocationState
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.After
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun tapSelectsSiteAndEmptyMapTapClearsSelection() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val site = sampleSite()
        val viewModel = MapViewModel(FakeMapSiteRepository(listOf(site)), FakeMapLocationProvider())
        val state = viewModel.uiState
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect() }
        advanceUntilIdle()

        viewModel.onSiteTap("site-1")
        advanceUntilIdle()
        assertEquals(site, state.value.selectedSite)

        viewModel.onSiteTap("")
        advanceUntilIdle()
        assertNull(state.value.selectedSite)

        collection.cancel()
        runCurrent()
    }

    @Test
    fun locationInputShowsInsideAndOutsideDistance() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val site = sampleSite()
        val locations = FakeMapLocationProvider()
        val viewModel = MapViewModel(FakeMapSiteRepository(listOf(site)), locations)
        val state = viewModel.uiState
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect() }
        advanceUntilIdle()

        viewModel.onSiteTap(site.id)
        viewModel.setLocationTrackingEnabled(true)
        locations.emit(LocationState.Fix(lat = 0.005, lng = 0.005, accuracyM = 8f))
        advanceUntilIdle()
        assertEquals(LatLng(0.005, 0.005), state.value.userLocation)
        assertEquals(true, state.value.isUserInsideSelectedSite)
        assertEquals(true, state.value.canCheckIn)
        assertEquals(0.0, state.value.distanceToSelectedSiteM)

        locations.emit(LocationState.Fix(lat = 0.02, lng = 0.005, accuracyM = 8f))
        advanceUntilIdle()
        assertEquals(false, state.value.isUserInsideSelectedSite)
        assertEquals(false, state.value.canCheckIn)
        assertEquals(true, state.value.distanceToSelectedSiteM!! > 1_000.0)

        collection.cancel()
        runCurrent()
    }
}

private fun sampleSite() = Site(
    id = "site-1", name = "Park", category = "Park", status = SiteStatus.ACTIVE,
    boundary = listOf(LatLng(0.0, 0.0), LatLng(0.0, 0.01), LatLng(0.01, 0.01), LatLng(0.01, 0.0)),
    areaSqm = 120.0, perimeterM = 0.0,
    lastInspectionAt = null, createdAt = 0L, updatedAt = 0L,
    serverVersion = 0L, syncState = SyncState.SYNCED
)

private class FakeMapSiteRepository(sites: List<Site>) : SiteRepository {
    private val siteFlow = MutableStateFlow(sites)
    override fun observeSites(query: String?, status: SiteStatus?): Flow<List<Site>> = siteFlow
    override fun getSiteById(id: String): Flow<Site?> = MutableStateFlow(siteFlow.value.firstOrNull { it.id == id })
    override suspend fun saveSite(site: Site) = Unit
    override suspend fun seedDatabaseIfEmpty() = Unit
}

private class FakeMapLocationProvider : LocationProvider {
    private val state = MutableStateFlow<LocationState>(LocationState.NoPermission)
    override fun locationUpdates(): Flow<LocationState> = state
    fun emit(value: LocationState) { state.value = value }
}
