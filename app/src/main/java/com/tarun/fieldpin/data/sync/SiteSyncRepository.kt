package com.tarun.fieldpin.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tarun.fieldpin.data.local.dao.SiteDao
import com.tarun.fieldpin.data.local.mapper.toDomain
import com.tarun.fieldpin.data.local.mapper.toEntity
import com.tarun.fieldpin.data.remote.SitePushResult
import com.tarun.fieldpin.data.remote.SiteRemoteDataSource
import com.tarun.fieldpin.domain.model.SyncState

class SiteSyncRepository(
    private val context: Context,
    private val siteDao: SiteDao,
    private val remote: SiteRemoteDataSource
) {
    suspend fun sync(): String {
        if (!hasNetwork()) return "Offline — pending sites are saved and ready to retry."
        val rows = siteDao.getSitesNeedingSync()
        if (rows.isEmpty()) return "Everything is synced."
        var succeeded = 0
        var failed = 0
        rows.forEach { row ->
            when (val result = remote.push(row.toDomain())) {
                is SitePushResult.Accepted -> {
                    siteDao.insertSite(result.site.copy(syncState = SyncState.SYNCED).toEntity())
                    succeeded++
                }
                is SitePushResult.Conflict -> {
                    // Store the server copy so an offline edit cannot silently overwrite it.
                    siteDao.insertSite(result.serverSite.copy(syncState = SyncState.FAILED).toEntity())
                    siteDao.setSyncFailure(row.id, SyncState.FAILED.name, "Conflict: server version kept. Review and edit again.")
                    failed++
                }
                is SitePushResult.Failed -> {
                    siteDao.setSyncFailure(row.id, SyncState.FAILED.name, result.reason)
                    failed++
                }
            }
        }
        return "$succeeded synced, $failed need retry."
    }

    private fun hasNetwork(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
