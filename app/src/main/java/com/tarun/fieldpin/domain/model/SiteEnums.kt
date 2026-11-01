package com.tarun.fieldpin.domain.model

enum class SiteStatus {
    ACTIVE,
    NEEDS_REPAIR,
    CLOSED
}

enum class InspectionStatus {
    DRAFT,
    SUBMITTED
}

enum class Condition {
    GOOD,
    FAIR,
    POOR
}

enum class SyncState{
    SYNCED,
    PENDING,
    FAILED
}