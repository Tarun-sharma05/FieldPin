package com.tarun.fieldpin.di

import androidx.room.Room
import com.tarun.fieldpin.data.local.AppDatabase
import com.tarun.fieldpin.data.local.MIGRATION_1_2
import com.tarun.fieldpin.data.local.seed.DatabaseSeeder
import com.tarun.fieldpin.data.remote.FakeSiteRemoteDataSource
import com.tarun.fieldpin.data.remote.SiteRemoteDataSource
import com.tarun.fieldpin.data.sync.SiteSyncRepository
import com.tarun.fieldpin.BuildConfig
import com.tarun.fieldpin.data.location.FusedLocationProvider
import com.tarun.fieldpin.data.repository.InspectionRepositoryImpl
import com.tarun.fieldpin.data.repository.SiteRepositoryImpl
import com.tarun.fieldpin.domain.repository.InspectionRepository
import com.tarun.fieldpin.domain.repository.LocationProvider
import com.tarun.fieldpin.domain.repository.SiteRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<LocationProvider> { FusedLocationProvider(androidContext()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "fieldpin.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    single { get<AppDatabase>().siteDao() }
    single { get<AppDatabase>().inspectionDao() }

    single { DatabaseSeeder(androidContext(), get()) }

    single<SiteRepository> { SiteRepositoryImpl(get(), get()) }
    single<InspectionRepository> { InspectionRepositoryImpl(get(), get()) }
    single<SiteRemoteDataSource> {
        FakeSiteRemoteDataSource(BuildConfig.FAKE_API_DELAY_MS, BuildConfig.FAKE_API_FAILURE_RATE)
    }
    single { SiteSyncRepository(androidContext(), get(), get()) }
}
