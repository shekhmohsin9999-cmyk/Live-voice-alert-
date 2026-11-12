package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppDistributionSection
import com.example.ui.components.PaymentItemCard
import com.example.ui.components.PaymentStatCards
import com.example.ui.components.SimulatePaymentDialog
import com.example.ui.components.SoundboxBanner
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer
import com.example.ui.theme.SoundboxBlue
import com.example.ui.viewmodel.SoundboxViewModel

@Composable
fun DashboardScreen(
    viewModel: SoundboxViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val recentPayments by viewModel.recentPayments.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeakingNow.collectAsStateWithLifecycle()

    var showSimulateDialog by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        hasNotificationPermission = viewModel.isNotificationAccessGranted(context)
    }

    if (showSimulateDialog) {
        SimulatePaymentDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { amt, customer, app ->
                viewModel.simulateIncomingPayment(amt, customer, app)
                Toast.makeText(
                    context,
                    "Payment of ₹$amt from $customer via ${app.displayName} added to History!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Merchant Store Header Info
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = SoundboxBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = profile.businessName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "UPI: ${profile.merchantVpa}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoundboxBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "ID: ${profile.deviceHardwareId.takeLast(8)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoundboxBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Notification Permission Warning (if not granted)
        if (!hasNotificationPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PendingAmberContainer),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = PendingAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Access Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Enable access to announce GPay, PhonePe & Paytm payments automatically.",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Opening Settings: Please toggle ON 'LIVE VOICE ALERT'",
                                    Toast.LENGTH_LONG
                                ).show()
                                viewModel.openNotificationSettings(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PendingAmber),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Soundbox Live Status Banner
        item {
            SoundboxBanner(
                profile = profile,
                isSpeaking = isSpeaking,
                onToggleSound = { enabled ->
                    viewModel.toggleVoiceAlerts(enabled)
                    Toast.makeText(
                        context,
                        if (enabled) "Voice Alerts: ON (Soundbox Active)" else "Voice Alerts: OFF (Muted)",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onTestClick = {
                    Toast.makeText(context, "Speaking: राहुल शर्मा से Google Pay पर ₹500 प्राप्त हुए...", Toast.LENGTH_SHORT).show()
                    viewModel.playTestVoice(500.0, "Google Pay", "राहुल शर्मा")
                },
                onSimulateClick = { showSimulateDialog = true }
            )
        }

        // 2. Collection Statistics Cards
        item {
            PaymentStatCards(stats = stats)
        }

        // 3. UPI App Breakdown Section
        item {
            AppDistributionSection(appStats = stats.appDistribution)
        }

        // 4. Recent Transactions Header with "View All"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Payments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "हाल के लेन-देन",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    modifier = Modifier
                        .clickable { onNavigateToHistory() }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "View All (${recentPayments.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoundboxBlue
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View all",
                        tint = SoundboxBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Recent Payments List (top 4 latest payments)
        val latestList = recentPayments.take(4)
        if (latestList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recent payments",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Simulate UPI' to test incoming payment voice alerts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(latestList, key = { it.id }) { payment ->
                PaymentItemCard(
                    payment = payment,
                    onReAnnounce = {
                        Toast.makeText(context, "Re-announcing ₹${payment.amount}...", Toast.LENGTH_SHORT).show()
                        viewModel.reAnnounce(payment)
                    }
                )
            }
        }
    }
}
