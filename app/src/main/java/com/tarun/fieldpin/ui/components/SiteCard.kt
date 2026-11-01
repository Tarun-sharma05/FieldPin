package com.tarun.fieldpin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tarun.fieldpin.domain.geometry.sqmToAcres
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSiteArea(sqm: Double): String {
    return if (sqm < 10_000.0) {
        String.format(Locale.US, "%,.0f m²", sqm)
    } else {
        val acres = sqmToAcres(sqm)
        String.format(Locale.US, "%.2f acres", acres)
    }
}

fun formatLastInspectionDate(epochMillis: Long?): String {
    if (epochMillis == null) return "Not inspected"
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return sdf.format(Date(epochMillis))
}

@Composable
fun SiteCard(
    site: Site,
    onSiteClick: (Site) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (site.status) {
        SiteStatus.ACTIVE -> Color(0xFF2E7D32)      // Green
        SiteStatus.NEEDS_REPAIR -> Color(0xFFED6C02) // Amber
        SiteStatus.CLOSED -> Color(0xFFD32F2F)       // Red
    }

    val statusText = when (site.status) {
        SiteStatus.ACTIVE -> "Active"
        SiteStatus.NEEDS_REPAIR -> "Needs Repair"
        SiteStatus.CLOSED -> "Closed"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSiteClick(site) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = site.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Area: ${formatSiteArea(site.areaSqm)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last Inspection: ${formatLastInspectionDate(site.lastInspectionAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (site.hasPendingSync) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Pending sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
