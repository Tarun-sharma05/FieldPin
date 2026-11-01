package com.tarun.fieldpin

import android.app.Application
import com.tarun.fieldpin.di.dataModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class FieldPinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@FieldPinApp)
            modules(dataModule)
        }
    }
}
