package com.tarun.fieldpin.data.local.entity

import android.R
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tarun.fieldpin.domain.model.Condition
import com.tarun.fieldpin.domain.model.InspectionStatus

@Entity(tableName = "inspection", foreignKeys = [ForeignKey(entity = SiteEntity::class, parentColumns = ["id"], childColumns = ["siteId"], onDelete = ForeignKey.CASCADE)], indices = [Index("siteId")])
data class InspectionEntity (
    @PrimaryKey
    val id: String,
    val siteId: String,
    val checkInAt: Long,
    val checkInLat: Double,
    val checkInLng: Double,
    val checkInAccuracyM: Double,
    val condition: String,
    val notes: String,
    val status: String,
    val createdAt: Long,
    val syncState: String
)