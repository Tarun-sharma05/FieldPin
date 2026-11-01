package com.tarun.fieldpin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tarun.fieldpin.data.local.entity.SiteEntity
import com.tarun.fieldpin.data.local.entity.SiteWithPendingCount
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {


    @Query("""
    SELECT site.*,
    (SELECT COUNT(*) FROM inspection WHERE siteId = site.id AND syncState != 'SYNCED') AS pendingInspectionsCount
    FROM site
    WHERE (:query IS NULL OR :query = '' OR name LIKE '%' || :query || '%')
    AND (:status IS NULL OR :status = '' OR status = :status)
    ORDER BY updatedAt DESC
    """)
    fun observeSites(query: String?, status: String?): Flow<List<SiteWithPendingCount>>

    @Query("""
        SELECT site.*,
           (SELECT COUNT(*) FROM inspection WHERE siteId = site.id AND syncState != 'SYNCED') AS pendingInspectionsCount
        FROM site
        WHERE id = :id
    """)
    fun getSiteById(id: String): Flow<SiteWithPendingCount?>

    @Query("SELECT COUNT(*) FROM site")
    suspend fun getSiteCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity)

    @Query("SELECT * FROM site WHERE syncState != 'SYNCED'")
    suspend fun getSitesNeedingSync(): List<SiteEntity>

    @Query("UPDATE site SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: String)

    @Query("UPDATE site SET syncState = :state, syncError = :reason WHERE id = :id")
    suspend fun setSyncFailure(id: String, state: String, reason: String)

}
