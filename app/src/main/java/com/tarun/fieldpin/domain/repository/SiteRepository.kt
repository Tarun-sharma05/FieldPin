package com.tarun.fieldpin.domain.repository

import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import kotlinx.coroutines.flow.Flow

interface SiteRepository {

    fun observeSites(query: String? = null, status: SiteStatus? = null): Flow<List<Site>>

    fun getSiteById(id: String): Flow<Site?>

    suspend fun saveSite(site: Site)

    suspend fun seedDatabaseIfEmpty()

}