package com.tarun.fieldpin.data.remote

import com.tarun.fieldpin.domain.model.Site

sealed interface SitePushResult {
    data class Accepted(val site: Site) : SitePushResult
    data class Conflict(val serverSite: Site) : SitePushResult
    data class Failed(val reason: String) : SitePushResult
}

interface SiteRemoteDataSource {
    suspend fun push(site: Site): SitePushResult
}
