package com.tarun.fieldpin.data.local.entity

import android.R
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "photo", foreignKeys = [ForeignKey(entity = InspectionEntity::class, parentColumns = ["id"], childColumns = ["inspectionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("inspectionId")])
data class PhotoEntity (
    @PrimaryKey
    val id: String,
    val inspectionId: String,
    val filePath: String,
    val takenAt: Long,
    val lat: Double,
    val lng: Double,
    val accuracyM: Double
)