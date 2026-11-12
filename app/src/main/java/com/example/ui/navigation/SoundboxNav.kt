package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector

enum class SoundboxTab(
    val title: String,
    val hindiTitle: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", "डैशबोर्ड", Icons.Default.Dashboard),
    HISTORY("History", "लेनदेन", Icons.AutoMirrored.Filled.ReceiptLong),
    SOUNDBOX("Voice Alert", "साउंडबॉक्स", Icons.AutoMirrored.Filled.VolumeUp),
    PROFILE("Profile", "दुकान", Icons.Default.Store),
    ADMIN("Admin & License", "एडमिन", Icons.Default.AdminPanelSettings)
}

sealed class SubScreen {
    object None : SubScreen()
    object PermissionGuide : SubScreen()
}
