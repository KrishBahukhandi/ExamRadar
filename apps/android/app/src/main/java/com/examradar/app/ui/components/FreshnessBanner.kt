package com.examradar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examradar.app.data.model.SourceFreshnessInfo
import com.examradar.app.util.DateFormatter

@Composable
fun FreshnessBanner(
    freshnessInfo: SourceFreshnessInfo,
    modifier: Modifier = Modifier
) {
    val bgColor = if (freshnessInfo.isStale) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
    val textColor = if (freshnessInfo.isStale) Color(0xFF991B1B) else Color(0xFF166534)
    val icon = if (freshnessInfo.isStale) Icons.Default.Warning else Icons.Default.Sync

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = freshnessInfo.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = DateFormatter.formatRelativeTime(freshnessInfo.lastSuccessfulSync),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun IndependenceDisclaimerBanner(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ExamRadar India is an independent examination information tracker. We are NOT affiliated with, sponsored, or endorsed by UPSC, SSC, IBPS, or any government agency. Always verify announcements against the official gazette notices.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
