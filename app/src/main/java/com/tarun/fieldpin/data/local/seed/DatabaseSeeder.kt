package com.tarun.fieldpin.data.local.seed

import android.content.Context
import com.tarun.fieldpin.data.local.converter.Converters
import com.tarun.fieldpin.data.local.dao.SiteDao
import com.tarun.fieldpin.data.local.entity.SiteEntity
import com.tarun.fieldpin.domain.geometry.polygonAreaSqm
import com.tarun.fieldpin.domain.geometry.polygonPerimeterM
import com.tarun.fieldpin.domain.model.LatLng
import kotlinx.serialization.json.Json

class DatabaseSeeder(
    private val context: Context,
    private val siteDao: SiteDao
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val converters = Converters()

    suspend fun seedIfEmpty() {
        if (siteDao.getSiteCount() > 0) return

        val jsonString = context.assets.open("seed_sites.json")
            .bufferedReader()
            .use { it.readText() }

        val dtos = json.decodeFromString<List<SeedSiteDto>>(jsonString)
        val now = System.currentTimeMillis()

        for (dto in dtos) {
            val domainBoundary = dto.boundary.map { LatLng(it.latitude, it.longitude) }
            require(domainBoundary.size >= 3) {
                "Seed site ${dto.id} polygon has fewer than 3 points!"
            }

            val area = polygonAreaSqm(domainBoundary)
            val perimeter = polygonPerimeterM(domainBoundary)

            val entity = SiteEntity(
                id = dto.id,
                name = dto.name,
                category = dto.category,
                status = dto.status,
                boundaryJson = converters.fromLatLngList(domainBoundary),
                areaSqm = area,
                perimeterM = perimeter,
                lastInspectionAt = null,
                createdAt = now,
                updatedAt = now,
                serverVersion = 1L,
                syncState = "SYNCED"
            )
            siteDao.insertSite(entity)
        }
    }
}