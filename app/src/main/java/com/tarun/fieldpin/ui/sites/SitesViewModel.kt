package com.tarun.fieldpin.ui.sites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.repository.SiteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SitesViewModel(
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<SiteStatus?>(null)

    init {
        viewModelScope.launch {
            siteRepository.seedDatabaseIfEmpty()
        }
    }

    private val debouncedQuery = _query.debounce(300L)

    private val searchParams = combine(
        _query.onStart { emit("") },
        debouncedQuery.onStart { emit("") },
        _selectedStatus
    ) { rawQuery, debouncedQ, status ->
        val effectiveQuery = if (rawQuery.isEmpty()) "" else debouncedQ
        Pair(effectiveQuery, status)
    }.distinctUntilChanged()

    val uiState: StateFlow<SitesUiState> = combine(
        _query,
        _selectedStatus,
        searchParams.flatMapLatest { (query, status) ->
            siteRepository.observeSites(
                query = query.ifBlank { null },
                status = status
            )
        }
    ) { query, status, sites ->
        SitesUiState(
            query = query,
            selectedStatus = status,
            sites = sites,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SitesUiState(isLoading = true)
    )

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    fun onStatusSelected(status: SiteStatus?) {
        _selectedStatus.value = status
    }
}
