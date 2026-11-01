package com.tarun.fieldpin.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InspectionWithPhotoEntity(
    @Embedded
    val inspection: InspectionEntity,
    @Relation(parentColumn = "id", entityColumn = "inspectionId")
    val photos: List<PhotoEntity>
)
