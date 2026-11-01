package com.tarun.fieldpin.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tarun.fieldpin.domain.model.SiteStatus

@Composable
fun FilterChipGroup(
    selectedStatus: SiteStatus?,
    onStatusSelected: (SiteStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedStatus == SiteStatus.ACTIVE,
            onClick = { onStatusSelected(SiteStatus.ACTIVE) },
            label = { Text("Active") }
        )
        FilterChip(
            selected = selectedStatus == SiteStatus.NEEDS_REPAIR,
            onClick = { onStatusSelected(SiteStatus.NEEDS_REPAIR) },
            label = { Text("Needs Repair") }
        )
        FilterChip(
            selected = selectedStatus == SiteStatus.CLOSED,
            onClick = { onStatusSelected(SiteStatus.CLOSED) },
            label = { Text("Closed") }
        )
    }
}
