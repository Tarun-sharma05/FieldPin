package com.tarun.fieldpin.domain.repository

import com.tarun.fieldpin.domain.model.LocationState
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    fun locationUpdates(): Flow<LocationState>
}
