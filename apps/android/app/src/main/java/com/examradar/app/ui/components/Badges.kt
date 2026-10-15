package com.examradar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.examradar.app.data.model.NoticeType
import com.examradar.app.data.model.VerificationStatus
import com.examradar.app.ui.theme.*

@Composable
fun VerificationBadge(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text, icon) = when (status) {
        VerificationStatus.VERIFIED -> Quad(
            StatusVerifiedBg, StatusVerified, "Official Verified", Icons.Default.CheckCircle
        )
        VerificationStatus.AUTOMATED_HIGH_CONFIDENCE -> Quad(
            Color(0xFFE0F2FE), Color(0xFF0369A1), "Automated Match", Icons.Default.CheckCircle
        )
        VerificationStatus.QUARANTINED -> Quad(
            StatusQuarantinedBg, StatusQuarantined, "Verification Required", Icons.Default.Warning
        )
        VerificationStatus.PENDING -> Quad(
            Color(0xFFF1F5F9), Color(0xFF64748B), "Pending Review", null
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = if (icon != null) 3.dp else 0.dp)
            )
        }
    }
}

@Composable
fun NoticeTypeBadge(
    noticeType: NoticeType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (noticeType) {
        NoticeType.RECRUITMENT -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), "Recruitment")
        NoticeType.ADMIT_CARD -> Triple(Color(0xFFF5F3FF), Color(0xFF6D28D9), "Admit Card")
        NoticeType.RESULT -> Triple(Color(0xFFECFDF5), Color(0xFF047857), "Result")
        NoticeType.ANSWER_KEY -> Triple(Color(0xFFFFFBEB), Color(0xFFB45309), "Answer Key")
        NoticeType.CORRIGENDUM -> Triple(StatusRevisedBg, StatusRevised, "Corrigendum")
        NoticeType.POSTPONEMENT -> Triple(DeadlineUrgentBg, DeadlineUrgent, "Postponed")
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeadlineBadge(
    text: String,
    isUrgent: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isUrgent) DeadlineUrgentBg else DeadlineApproachingBg
    val textColor = if (isUrgent) DeadlineUrgent else DeadlineApproaching

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
