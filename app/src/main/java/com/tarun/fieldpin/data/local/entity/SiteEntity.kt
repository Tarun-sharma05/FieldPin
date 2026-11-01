package com.tarun.fieldpin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "site", indices = [Index("name"), Index("status")])
data class SiteEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val status: String,
    val boundaryJson : String,
    val areaSqm : Double,
    val perimeterM: Double,
    val lastInspectionAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val serverVersion: Long,
    val syncState: String,
    val syncError: String? = null
)
