package com.tarun.fieldpin.data.local.mapper

import com.tarun.fieldpin.data.local.converter.Converters
import com.tarun.fieldpin.data.local.entity.InspectionEntity
import com.tarun.fieldpin.data.local.entity.InspectionWithPhotoEntity
import com.tarun.fieldpin.data.local.entity.PhotoEntity
import com.tarun.fieldpin.data.local.entity.SiteEntity
import com.tarun.fieldpin.data.local.entity.SiteWithPendingCount
import com.tarun.fieldpin.domain.model.Condition
import com.tarun.fieldpin.domain.model.Inspection
import com.tarun.fieldpin.domain.model.InspectionStatus
import com.tarun.fieldpin.domain.model.InspectionWithPhotos
import com.tarun.fieldpin.domain.model.Photo
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.domain.model.SyncState
private val converters = Converters()

// --- Site Mappers ---

fun SiteWithPendingCount.toDomain(): Site {
    val syncStateEnum = runCatching { SyncState.valueOf(site.syncState) }.getOrDefault(SyncState.SYNCED)
    val statusEnum = runCatching { SiteStatus.valueOf(site.status) }.getOrDefault(SiteStatus.ACTIVE)

    val isPending = syncStateEnum != SyncState.SYNCED || pendingInspectionsCount > 0

    return Site(
        id = site.id,
        name = site.name,
        category = site.category,
        status = statusEnum,
        boundary = converters.toLatLngList(site.boundaryJson),
        areaSqm = site.areaSqm,
        perimeterM = site.perimeterM,
        lastInspectionAt = site.lastInspectionAt,
        createdAt = site.createdAt,
        updatedAt = site.updatedAt,
        serverVersion = site.serverVersion,
        syncState = syncStateEnum,
        hasPendingSync = isPending
    )
}

fun SiteEntity.toDomain(): Site = SiteWithPendingCount(this, 0).toDomain()

fun Site.toEntity(): SiteEntity {
    return SiteEntity(
        id = id,
        name = name,
        category = category,
        status = status.name,
        boundaryJson = converters.fromLatLngList(boundary),
        areaSqm = areaSqm,
        perimeterM = perimeterM,
        lastInspectionAt = lastInspectionAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        serverVersion = serverVersion,
        syncState = syncState.name,
        syncError = null
    )
}

// --- Inspection Mappers ---

fun InspectionEntity.toDomain(): Inspection {
    return Inspection(
        id = id,
        siteId = siteId,
        checkInAt = checkInAt,
        checkInLat = checkInLat,
        checkInLng = checkInLng,
        checkInAccuracyM = checkInAccuracyM,
        condition = runCatching { Condition.valueOf(condition) }.getOrDefault(Condition.GOOD),
        notes = notes,
        status = runCatching { InspectionStatus.valueOf(status) }.getOrDefault(InspectionStatus.SUBMITTED),
        createdAt = createdAt,
        syncState = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.SYNCED)
    )
}

fun Inspection.toEntity(): InspectionEntity {
    return InspectionEntity(
        id = id,
        siteId = siteId,
        checkInAt = checkInAt,
        checkInLat = checkInLat,
        checkInLng = checkInLng,
        checkInAccuracyM = checkInAccuracyM,
        condition = condition.name,
        notes = notes,
        status = status.name,
        createdAt = createdAt,
        syncState = syncState.name
    )
}

// --- Photo Mappers ---

fun PhotoEntity.toDomain(): Photo {
    return Photo(
        id = id,
        inspectionId = inspectionId,
        filePath = filePath,
        takenAt = takenAt,
        lat = lat,
        lng = lng,
        accuracyM = accuracyM
    )
}

fun Photo.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = id,
        inspectionId = inspectionId,
        filePath = filePath,
        takenAt = takenAt,
        lat = lat,
        lng = lng,
        accuracyM = accuracyM
    )
}

// --- InspectionWithPhotos Mapper ---

fun InspectionWithPhotoEntity.toDomain(): InspectionWithPhotos {
    return InspectionWithPhotos(
        inspection = inspection.toDomain(),
        photos = photos.map { it.toDomain() }
    )
}
