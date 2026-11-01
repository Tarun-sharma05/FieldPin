package com.tarun.fieldpin.ui.sites

import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus

data class SitesUiState(
    val query: String = "",
    val selectedStatus: SiteStatus? = null,
    val sites: List<Site> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)
