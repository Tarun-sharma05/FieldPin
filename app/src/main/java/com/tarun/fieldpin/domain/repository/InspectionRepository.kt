package com.tarun.fieldpin.domain.repository

import com.tarun.fieldpin.domain.model.Inspection
import com.tarun.fieldpin.domain.model.InspectionWithPhotos
import com.tarun.fieldpin.domain.model.Photo
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun observeInspectionForSite(siteId: String): Flow<List<InspectionWithPhotos>>

    suspend fun saveInspection(inspection: Inspection, photos: List<Photo>)
}