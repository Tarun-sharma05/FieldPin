package com.tarun.fieldpin.data.repository

import com.tarun.fieldpin.data.local.dao.SiteDao
import com.tarun.fieldpin.data.local.mapper.toDomain
import com.tarun.fieldpin.data.local.mapper.toEntity
import com.tarun.fieldpin.data.local.seed.DatabaseSeeder
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.repository.SiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SiteRepositoryImpl(
    private val siteDao: SiteDao,
    private val databaseSeeder: DatabaseSeeder
) : SiteRepository {

    override fun observeSites(query: String?, status: SiteStatus?): Flow<List<Site>> {
        return siteDao.observeSites(query = query, status = status?.name)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getSiteById(id: String): Flow<Site?> {
        return siteDao.getSiteById(id)
            .map { it?.toDomain() }
    }

    override suspend fun saveSite(site: Site) {
        val now = System.currentTimeMillis()
        val siteToSave = site.copy(
            updatedAt = now,
            syncState = SyncState.PENDING
        )
        siteDao.insertSite(siteToSave.toEntity())
    }

    override suspend fun seedDatabaseIfEmpty() {
        databaseSeeder.seedIfEmpty()
    }
}