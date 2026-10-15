package com.examradar.app.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examradar.app.ui.components.IndependenceDisclaimerBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy & Terms") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            IndependenceDisclaimerBanner()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "1. Data Minimization & Security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ExamRadar India is designed to protect candidate privacy. We strictly do NOT collect, store, or process:\n\n" +
                        "• Aadhaar numbers, voter IDs, or government identity cards.\n" +
                        "• Official examination portal passwords, application login credentials, or OTPs.\n" +
                        "• Bank account details, UPI identifiers, or credit/debit card numbers.\n" +
                        "• Scanned identity documents, photographs, or signature uploads.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "2. Why Profile Information is Requested",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When you choose your graduation status or educational qualification stream, that data is stored locally to match your profile against published notification cut-offs (such as degree requirements or tentative age limits). It is never shared with third parties or advertisers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "3. Official Portal Redirection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ExamRadar provides direct, unmanipulated hyperlinks to original official notices (e.g. upsc.gov.in, ssc.gov.in, ibps.in). All applications and examination fee transactions must be conducted exclusively on the official recruiting authority's HTTPS website.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "4. Account and Data Erasure",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "You can delete all locally persisted examination targets, checklists, and notification preferences at any time via Account & Privacy > Right to be Forgotten.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}
