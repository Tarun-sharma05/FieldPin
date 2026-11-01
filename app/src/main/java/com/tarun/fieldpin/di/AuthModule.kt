package com.tarun.fieldpin.di

import com.tarun.fieldpin.data.repository.FakeAuthRepository
import com.tarun.fieldpin.data.session.DataStoreSessionStore
import com.tarun.fieldpin.data.session.SessionStore
import com.tarun.fieldpin.domain.repository.AuthRepository
import com.tarun.fieldpin.ui.login.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<SessionStore> { DataStoreSessionStore(androidContext()) }
    single<AuthRepository> { FakeAuthRepository(get(), get(), androidContext()) }
    viewModel { LoginViewModel(get()) }
}
