package com.tarun.fieldpin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tarun.fieldpin.data.local.entity.InspectionEntity
import com.tarun.fieldpin.data.local.entity.InspectionWithPhotoEntity
import com.tarun.fieldpin.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    @Transaction
    @Query("SELECT * FROM inspection WHERE siteId = :siteId ORDER BY createdAt DESC")
    fun observeInspectionsForSite(siteId: String): Flow<List<InspectionWithPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: InspectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)


}