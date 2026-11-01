package com.tarun.fieldpin.ui.sites

import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.repository.SiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SitesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSiteRepository
    private lateinit var viewModel: SitesViewModel

    private val sampleSites = listOf(
        Site(
            id = "1",
            name = "Central Park",
            category = "Park",
            status = SiteStatus.ACTIVE,
            boundary = listOf(LatLng(12.971, 77.594), LatLng(12.972, 77.594), LatLng(12.971, 77.595)),
            areaSqm = 8000.0,
            perimeterM = 400.0,
            lastInspectionAt = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L,
            serverVersion = 1L,
            syncState = SyncState.SYNCED
        ),
        Site(
            id = "2",
            name = "Water Tank Alpha",
            category = "Water Tank",
            status = SiteStatus.NEEDS_REPAIR,
            boundary = listOf(LatLng(12.981, 77.604), LatLng(12.982, 77.604), LatLng(12.981, 77.605)),
            areaSqm = 12000.0,
            perimeterM = 500.0,
            lastInspectionAt = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            serverVersion = 1L,
            syncState = SyncState.PENDING,
            hasPendingSync = true
        ),
        Site(
            id = "3",
            name = "Solar Site 1",
            category = "Solar",
            status = SiteStatus.CLOSED,
            boundary = listOf(LatLng(12.991, 77.614), LatLng(12.992, 77.614), LatLng(12.991, 77.615)),
            areaSqm = 15000.0,
            perimeterM = 600.0,
            lastInspectionAt = 2000L,
            createdAt = 1000L,
            updatedAt = 1000L,
            serverVersion = 1L,
            syncState = SyncState.SYNCED
        ),
        Site(
            id = "4",
            name = "Central Park North",
            category = "Park",
            status = SiteStatus.NEEDS_REPAIR,
            boundary = listOf(LatLng(12.975, 77.595), LatLng(12.976, 77.595), LatLng(12.975, 77.596)),
            areaSqm = 5000.0,
            perimeterM = 300.0,
            lastInspectionAt = 1500L,
            createdAt = 1000L,
            updatedAt = 1000L,
            serverVersion = 1L,
            syncState = SyncState.SYNCED
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSiteRepository()
        fakeRepository.setSites(sampleSites)
        viewModel = SitesViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_triggersSeedAndLoadsAllSites() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        assertTrue(fakeRepository.seedDatabaseCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(4, state.sites.size)
    }

    @Test
    fun queryOnly_filtersSitesByName() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.onQueryChanged("Central")
        advanceTimeBy(350L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Central", state.query)
        assertEquals(2, state.sites.size)
        assertTrue(state.sites.all { it.name.contains("Central") })
    }

    @Test
    fun statusOnly_filtersSitesByStatus() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.onStatusSelected(SiteStatus.NEEDS_REPAIR)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SiteStatus.NEEDS_REPAIR, state.selectedStatus)
        assertEquals(2, state.sites.size)
        assertTrue(state.sites.all { it.status == SiteStatus.NEEDS_REPAIR })
    }

    @Test
    fun queryAndStatusCombined_filtersSitesByBoth() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.onQueryChanged("Central")
        viewModel.onStatusSelected(SiteStatus.NEEDS_REPAIR)
        advanceTimeBy(350L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.sites.size)
        assertEquals("Central Park North", state.sites.first().name)
    }

    @Test
    fun noMatch_returnsEmptyList() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        viewModel.onQueryChanged("NonExistentSite")
        advanceTimeBy(350L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.sites.isEmpty())
    }

    private class FakeSiteRepository : SiteRepository {
        private val sitesFlow = MutableStateFlow<List<Site>>(emptyList())
        var seedDatabaseCalled = false

        fun setSites(sites: List<Site>) {
            sitesFlow.value = sites
        }

        override fun observeSites(query: String?, status: SiteStatus?): Flow<List<Site>> {
            return sitesFlow.map { list ->
                list.filter { site ->
                    val matchesQuery = query.isNullOrBlank() || site.name.contains(query, ignoreCase = true)
                    val matchesStatus = status == null || site.status == status
                    matchesQuery && matchesStatus
                }
            }
        }

        override fun getSiteById(id: String): Flow<Site?> {
            return sitesFlow.map { list -> list.find { it.id == id } }
        }

        override suspend fun saveSite(site: Site) {
            sitesFlow.value = sitesFlow.value + site
        }

        override suspend fun seedDatabaseIfEmpty() {
            seedDatabaseCalled = true
        }
    }
}
