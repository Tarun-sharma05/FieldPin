package com.tarun.fieldpin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tarun.fieldpin.data.local.converter.Converters
import com.tarun.fieldpin.data.local.dao.InspectionDao
import com.tarun.fieldpin.data.local.dao.SiteDao
import com.tarun.fieldpin.data.local.entity.InspectionEntity
import com.tarun.fieldpin.data.local.entity.PhotoEntity
import com.tarun.fieldpin.data.local.entity.SiteEntity

@Database(
    entities = [
        SiteEntity::class,
        InspectionEntity::class,
        PhotoEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun inspectionDao(): InspectionDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE site ADD COLUMN syncError TEXT")
    }
}
