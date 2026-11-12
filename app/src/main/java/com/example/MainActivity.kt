package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Surface
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.service.SoundboxForegroundService
import com.example.ui.components.SimulatePaymentDialog
import com.example.ui.navigation.SoundboxTab
import com.example.ui.navigation.SubScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MerchantProfileScreen
import com.example.ui.screens.PermissionGuideScreen
import com.example.ui.screens.SoundboxSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SoundboxBlue
import com.example.ui.viewmodel.SoundboxViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SoundboxViewModel by viewModels {
        SoundboxViewModel.provideFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: SoundboxViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(SoundboxTab.DASHBOARD) }
    var currentSubScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
    var showSimulateGlobalDialog by remember { mutableStateOf(false) }

    val profile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val currentAnnouncement by viewModel.currentAnnouncement.collectAsStateWithLifecycle()

    // Handle system back navigation: pops sub-screen first, otherwise returns to Dashboard
    BackHandler(enabled = currentSubScreen !is SubScreen.None || selectedTab != SoundboxTab.DASHBOARD) {
        if (currentSubScreen !is SubScreen.None) {
            currentSubScreen = SubScreen.None
        } else if (selectedTab != SoundboxTab.DASHBOARD) {
            selectedTab = SoundboxTab.DASHBOARD
        }
    }

    if (showSimulateGlobalDialog) {
        SimulatePaymentDialog(
            onDismiss = { showSimulateGlobalDialog = false },
            onSimulate = { amount, customer, app ->
                viewModel.simulateIncomingPayment(amount, customer, app)
                Toast.makeText(
                    context,
                    "Simulated ₹$amount via ${app.displayName}. Added to History!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentSubScreen is SubScreen.None) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SoundboxBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LIVE VOICE ALERT",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ध्वनि सूचना साउंडबॉक्स",
                                    fontSize = 10.sp,
                                    color = SoundboxBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSimulateGlobalDialog = true },
                            modifier = Modifier.testTag("global_simulate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = "Simulate payment",
                                tint = SoundboxBlue
                            )
                        }

                        IconButton(
                            onClick = { currentSubScreen = SubScreen.PermissionGuide },
                            modifier = Modifier.testTag("global_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Permissions guide",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("soundbox_bottom_navigation"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                SoundboxTab.entries.forEach { tab ->
                    val isSelected = (selectedTab == tab) && (currentSubScreen is SubScreen.None)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                            currentSubScreen = SubScreen.None
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SoundboxBlue,
                            selectedTextColor = SoundboxBlue,
                            indicatorColor = SoundboxBlue.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // High-visibility animated voice alert banner
            AnimatedVisibility(
                visible = currentAnnouncement != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF059669),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔊 SOUNDBOX VOICE ALERT (ध्वनि घोषणा)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = currentAnnouncement ?: "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
            if (currentSubScreen is SubScreen.PermissionGuide) {
                PermissionGuideScreen(
                    viewModel = viewModel,
                    onBack = { currentSubScreen = SubScreen.None }
                )
            } else {
                when (selectedTab) {
                    SoundboxTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToHistory = {
                                selectedTab = SoundboxTab.HISTORY
                                currentSubScreen = SubScreen.None
                            },
                            onNavigateToPermissions = {
                                currentSubScreen = SubScreen.PermissionGuide
                            }
                        )
                    }
                    SoundboxTab.HISTORY -> {
                        HistoryScreen(viewModel = viewModel)
                    }
                    SoundboxTab.SOUNDBOX -> {
                        SoundboxSettingsScreen(
                            viewModel = viewModel,
                            onNavigateToPermissionGuide = {
                                currentSubScreen = SubScreen.PermissionGuide
                            }
                        )
                    }
                    SoundboxTab.PROFILE -> {
                        MerchantProfileScreen(
                            viewModel = viewModel,
                            onNavigateToAdmin = {
                                selectedTab = SoundboxTab.ADMIN
                                currentSubScreen = SubScreen.None
                            }
                        )
                    }
                    SoundboxTab.ADMIN -> {
                        AdminPanelScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
}
