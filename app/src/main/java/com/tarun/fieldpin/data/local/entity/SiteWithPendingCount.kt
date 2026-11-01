package com.tarun.fieldpin.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity

data class SiteWithPendingCount (
    @Embedded
    val site: SiteEntity,
    val pendingInspectionsCount: Int
)