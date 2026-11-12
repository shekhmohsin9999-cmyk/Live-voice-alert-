package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer
import com.example.ui.theme.SoundboxBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.SoundboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideScreen(
    viewModel: SoundboxViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isNotificationAccessGranted by remember { mutableStateOf(false) }
    var isBatteryOptIgnored by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        isNotificationAccessGranted = viewModel.isNotificationAccessGranted(context)
        isBatteryOptIgnored = viewModel.isIgnoringBatteryOptimizations(context)
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Permission & Soundbox Architecture",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Why these Android permissions are required",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("permission_guide_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SoundboxBlue.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoundboxBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SoundboxBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "LIVE VOICE ALERT transforms your Android smartphone into a commercial payment soundbox. Android OS requires explicit permissions for security and background voice operation.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            // 1. Notification Access (CRUCIAL)
            PermissionDetailCard(
                title = "1. Notification Access Service",
                permissionName = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                description = "Google Pay, PhonePe, and Paytm deliver payment receipts directly via Android system notifications. By granting Notification Access, the Soundbox service instantly captures incoming payment amounts (e.g. ₹500) and payer names without needing access to your banking passwords or SMS.",
                isGranted = isNotificationAccessGranted,
                actionButtonText = if (isNotificationAccessGranted) "Access Granted (Verified)" else "Grant in Android Settings",
                onActionClick = {
                    Toast.makeText(
                        context,
                        "Opening Android Settings: Please find and toggle ON 'LIVE VOICE ALERT'",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.openNotificationSettings(context)
                    refreshPermissions()
                }
            )

            // 2. Battery Optimization Ignore
            PermissionDetailCard(
                title = "2. Ignore Battery Optimization",
                permissionName = "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                description = "Modern Android smartphones (especially Xiaomi, Samsung, OnePlus, Vivo) automatically put background soundbox apps to sleep after a few minutes of screen inactivity. Disabling battery optimization guarantees that your soundbox speaks every single payment 24/7 without delays.",
                isGranted = isBatteryOptIgnored,
                actionButtonText = if (isBatteryOptIgnored) "Optimization Disabled (Active 24/7)" else "Disable Optimization",
                onActionClick = {
                    Toast.makeText(
                        context,
                        "Opening Battery Settings: Choose 'Don't Optimize' for Live Voice Alert",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.requestIgnoreBatteryOptimizations(context)
                    refreshPermissions()
                }
            )

            // 3. Wake Lock Permission
            PermissionDetailCard(
                title = "3. Wake Lock (WAKE_LOCK)",
                permissionName = "android.permission.WAKE_LOCK",
                description = "When a customer pays while your phone is locked on the counter, WakeLock allows the device CPU to momentarily wake up, trigger the TextToSpeech engine, announce \"₹500 प्राप्त हुए\", and then gently let the phone return to sleep to conserve battery.",
                isGranted = true, // Normal install-time permission
                actionButtonText = "System Installed & Active",
                onActionClick = null
            )

            // 4. Foreground Service & Audio Modification
            PermissionDetailCard(
                title = "4. Media Playback & Volume Control",
                permissionName = "FOREGROUND_SERVICE_MEDIA_PLAYBACK & MODIFY_AUDIO_SETTINGS",
                description = "Enables continuous foreground operation compliant with Android 14+ background rules and boosts speaker volume level to ensure loud, crisp announcements in bustling, noisy merchant shops.",
                isGranted = true,
                actionButtonText = "System Integrated",
                onActionClick = null
            )

            // 5. Boot Completed
            PermissionDetailCard(
                title = "5. Auto-Start on Reboot",
                permissionName = "android.permission.RECEIVE_BOOT_COMPLETED",
                description = "Ensures that whenever your shop phone restarts or powers on after being charged, the soundbox listener automatically resumes standby mode without needing manual intervention.",
                isGranted = true,
                actionButtonText = "System Integrated",
                onActionClick = null
            )
        }
    }
}

@Composable
fun PermissionDetailCard(
    title: String,
    permissionName: String,
    description: String,
    isGranted: Boolean,
    actionButtonText: String,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGranted) SuccessGreenContainer else PendingAmberContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isGranted) SuccessGreen else PendingAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isGranted) "Granted" else "Action Required",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) SuccessGreen else PendingAmber
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = permissionName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            if (onActionClick != null) {
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGranted) SuccessGreen else SoundboxBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = actionButtonText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
