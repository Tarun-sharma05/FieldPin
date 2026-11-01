package com.tarun.fieldpin.ui.newsite

import com.tarun.fieldpin.domain.geometry.polygonAreaSqm
import com.tarun.fieldpin.domain.geometry.polygonPerimeterM
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.repository.SiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NewSiteViewModelTest {
    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun addingAndUndoingCornersUpdatesMetricsAndSaveEligibility() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = NewSiteViewModel(FakeNewSiteRepository())
        val corners = sampleBoundary()

        assertFalse(viewModel.uiState.value.canSave)
        corners.take(3).forEach(viewModel::onMapTap)
        val complete = viewModel.uiState.value
        assertEquals(corners.take(3), complete.boundary)
        assertEquals(polygonAreaSqm(corners.take(3)), complete.areaSqm)
        assertTrue(complete.areaAcres > 0.0)
        assertEquals(polygonPerimeterM(corners.take(3)), complete.perimeterM)
        assertTrue(complete.canSave)

        viewModel.undoLastCorner()
        val undone = viewModel.uiState.value
        assertEquals(2, undone.boundary.size)
        assertEquals(0.0, undone.areaSqm)
        assertEquals(0.0, undone.perimeterM)
        assertFalse(undone.canSave)
    }

    @Test
    fun savingThreeOrMoreCornersPersistsPendingSite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeNewSiteRepository()
        val viewModel = NewSiteViewModel(repository)
        sampleBoundary().forEach(viewModel::onMapTap)
        viewModel.saveSite()
        advanceUntilIdle()

        val saved = repository.saved.single()
        assertEquals("New site", saved.name)
        assertEquals("Park", saved.category)
        assertEquals(SiteStatus.ACTIVE, saved.status)
        assertEquals(SyncState.PENDING, saved.syncState)
        assertEquals(sampleBoundary(), saved.boundary)
        assertEquals(true, viewModel.uiState.value.isSaved)
    }
}

private fun sampleBoundary() = listOf(
    LatLng(26.9124, 75.7873),
    LatLng(26.9124, 75.7883),
    LatLng(26.9134, 75.7883),
    LatLng(26.9134, 75.7873)
)

private class FakeNewSiteRepository : SiteRepository {
    private val sites = MutableStateFlow<List<Site>>(emptyList())
    val saved = mutableListOf<Site>()

    override fun observeSites(query: String?, status: SiteStatus?): Flow<List<Site>> = sites
    override fun getSiteById(id: String): Flow<Site?> = MutableStateFlow(sites.value.firstOrNull { it.id == id })
    override suspend fun saveSite(site: Site) {
        saved += site
        sites.value = sites.value + site
    }
    override suspend fun seedDatabaseIfEmpty() = Unit
}
