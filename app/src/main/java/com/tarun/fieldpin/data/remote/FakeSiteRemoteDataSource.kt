package com.tarun.fieldpin.data.remote

import com.tarun.fieldpin.domain.model.Site
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeSiteRemoteDataSource(
    private val delayMs: Long,
    private val failureRate: Float,
    private val random: Random = Random.Default
) : SiteRemoteDataSource {
    private val serverSites = mutableMapOf<String, Site>()

    override suspend fun push(site: Site): SitePushResult {
        delay(delayMs)
        if (random.nextFloat() < failureRate) return SitePushResult.Failed("Temporary sync failure. Try again.")
        val current = serverSites[site.id]
        if (current != null && current.serverVersion != site.serverVersion) {
            return SitePushResult.Conflict(current)
        }
        val accepted = site.copy(serverVersion = site.serverVersion + 1)
        serverSites[site.id] = accepted
        return SitePushResult.Accepted(accepted)
    }
}
