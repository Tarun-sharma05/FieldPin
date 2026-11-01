package com.tarun.fieldpin.data.repository

import com.tarun.fieldpin.data.local.dao.InspectionDao
import com.tarun.fieldpin.data.local.dao.SiteDao
import com.tarun.fieldpin.data.local.mapper.toDomain
import com.tarun.fieldpin.data.local.mapper.toEntity
import com.tarun.fieldpin.domain.model.Inspection
import com.tarun.fieldpin.domain.model.InspectionWithPhotos
import com.tarun.fieldpin.domain.model.Photo
import com.tarun.fieldpin.domain.model.SyncState
import com.tarun.fieldpin.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class InspectionRepositoryImpl(
    private val inspectionDao: InspectionDao,
    private val siteDao: SiteDao
) : InspectionRepository {

    override fun observeInspectionForSite(siteId: String): Flow<List<InspectionWithPhotos>> {
        return inspectionDao.observeInspectionsForSite(siteId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveInspection(inspection: Inspection, photos: List<Photo>) {
        val now = System.currentTimeMillis()
        val inspectionToSave = inspection.copy(
            syncState = SyncState.PENDING
        )

        // 1. Insert inspection and photos
        inspectionDao.insertInspection(inspectionToSave.toEntity())
        if (photos.isNotEmpty()) {
            inspectionDao.insertPhotos(photos.map { it.toEntity() })
        }

        // 2. Update parent site's lastInspectionAt, updatedAt, and mark as PENDING
        val existingSiteWithPending = siteDao.getSiteById(inspection.siteId).firstOrNull()
        if (existingSiteWithPending != null) {
            val updatedSiteEntity = existingSiteWithPending.site.copy(
                lastInspectionAt = inspection.checkInAt,
                updatedAt = now,
                syncState = SyncState.PENDING.name
            )
            siteDao.insertSite(updatedSiteEntity)
        }
    }
}