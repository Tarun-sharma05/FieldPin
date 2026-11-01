package com.tarun.fieldpin.di

import com.tarun.fieldpin.ui.sites.SitesViewModel
import com.tarun.fieldpin.ui.map.MapViewModel
import com.tarun.fieldpin.ui.newsite.NewSiteViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { SitesViewModel(get()) }
    viewModel { MapViewModel(get(), get()) }
    viewModel { NewSiteViewModel(get()) }
}
