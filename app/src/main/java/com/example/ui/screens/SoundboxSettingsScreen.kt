package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AnnouncementTemplate
import com.example.data.model.VoiceLanguage
import com.example.ui.theme.SoundboxBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.SoundboxViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundboxSettingsScreen(
    viewModel: SoundboxViewModel,
    onNavigateToPermissionGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeakingNow.collectAsStateWithLifecycle()

    var isNotificationAccessGranted by remember { mutableStateOf(false) }
    var isBatteryOptIgnored by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isNotificationAccessGranted = viewModel.isNotificationAccessGranted(context)
        isBatteryOptIgnored = viewModel.isIgnoringBatteryOptimizations(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("soundbox_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Sound Alert Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (profile.voiceAlertsEnabled) SoundboxBlue else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Sound Alert Announcement",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (profile.voiceAlertsEnabled) "Enabled: Speaks payment alerts" else "Disabled: Sound muted",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Switch(
                    checked = profile.voiceAlertsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.toggleVoiceAlerts(enabled)
                        Toast.makeText(
                            context,
                            if (enabled) "Voice Alerts: ON (Soundbox Active)" else "Voice Alerts: OFF (Muted)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("settings_sound_toggle"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SoundboxBlue,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Test Voice Alert Button Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
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
                        text = "Voice Output Preview",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSpeaking) {
                        Surface(
                            color = SuccessGreenContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PLAYING NOW",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = SoundboxBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "\"${profile.announcementTemplate.render("500", "Google Pay", "राहुल", profile.selectedLanguage)}\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Speaking: राहुल शर्मा से Paytm पर ₹500 प्राप्त हुए...", Toast.LENGTH_SHORT).show()
                        viewModel.playTestVoice(
                            amount = 500.0,
                            upiApp = "Paytm",
                            customer = "राहुल शर्मा"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_voice_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoundboxBlue)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Voice: \"राहुल शर्मा से ₹500 प्राप्त हुए\"")
                }
            }
        }

        // Language Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = SoundboxBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Announcement Language",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceLanguage.entries.forEach { lang ->
                        val isSelected = profile.selectedLanguage == lang
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SoundboxBlue else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.setVoiceLanguage(lang)
                                    Toast.makeText(context, "Language set to ${lang.displayName}", Toast.LENGTH_SHORT).show()
                                },
                            color = if (isSelected) SoundboxBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = lang.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SoundboxBlue else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Announcement Template Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Announcement Format / Phrase",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                AnnouncementTemplate.entries.forEach { template ->
                    val isSelected = profile.announcementTemplate == template
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SoundboxBlue else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.setAnnouncementTemplate(template)
                                Toast.makeText(context, "Template: ${template.title}", Toast.LENGTH_SHORT).show()
                            },
                        color = if (isSelected) SoundboxBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = template.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) SoundboxBlue else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SoundboxBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Preview: \"${template.render("500", "Google Pay", "राहुल", profile.selectedLanguage)}\"",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Audio Controls: Volume, Speech Rate, Vibration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Sound & Audio Controls",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Volume Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Speaker Volume Level", fontSize = 13.sp)
                        Text(
                            text = "${profile.volumeLevel}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoundboxBlue
                        )
                    }
                    Slider(
                        value = profile.volumeLevel.toFloat(),
                        onValueChange = { viewModel.setVolume(it.toInt()) },
                        valueRange = 10f..100f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = SoundboxBlue,
                            activeTrackColor = SoundboxBlue
                        )
                    )
                }

                // Speech Rate Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Speech Rate (गति)", fontSize = 13.sp)
                        Text(
                            text = "${String.format("%.2f", profile.speechRate)}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoundboxBlue
                        )
                    }
                    Slider(
                        value = profile.speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.7f..1.4f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = SoundboxBlue,
                            activeTrackColor = SoundboxBlue
                        )
                    )
                }

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = SoundboxBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(text = "Haptic Vibration", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Vibrate on payment detection", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Switch(
                        checked = profile.vibrateOnPayment,
                        onCheckedChange = { enabled ->
                            viewModel.setVibrateOnPayment(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Vibration feedback ON" else "Vibration feedback OFF",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SoundboxBlue
                        )
                    )
                }
            }
        }

        // Permissions & Background Operation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPermissionGuide() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SoundboxBlue.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SoundboxBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Soundbox Permissions & Readiness",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Notification Access, WakeLock & Battery Optimization",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open permissions",
                    tint = SoundboxBlue
                )
            }
        }
    }
}
