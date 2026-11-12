package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.LicenseStatus
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer
import com.example.ui.theme.RefundRed
import com.example.ui.theme.RefundRedContainer
import com.example.ui.theme.SoundboxBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.SoundboxViewModel

@Composable
fun AdminPanelScreen(
    viewModel: SoundboxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val license by viewModel.deviceLicense.collectAsStateWithLifecycle()
    val merchants by viewModel.registeredMerchants.collectAsStateWithLifecycle()
    val adminMessage by viewModel.adminMessage.collectAsStateWithLifecycle()

    var inputKey by remember { mutableStateOf("") }
    var showAddMerchantDialog by remember { mutableStateOf(false) }

    if (showAddMerchantDialog) {
        AddMerchantDialog(
            onDismiss = { showAddMerchantDialog = false },
            onAdd = { name, owner, phone, devId ->
                viewModel.addMerchant(name, owner, phone, devId)
                Toast.makeText(context, "Merchant registered with ₹100 license!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_panel_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ₹100 Single-Device Architecture Explanation Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = SoundboxBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "₹100 Single-Device Licensing Architecture",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "Each commercial soundbox license is priced at ₹100 and is cryptographically bound to the phone's unique hardware Device ID. One license key activates exactly one terminal device and cannot be shared or pirated across multiple stores.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    lineHeight = 17.sp
                )
            }
        }

        // Current Device License Card
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This Device License Status",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (license.status) {
                            LicenseStatus.ACTIVE -> SuccessGreenContainer
                            LicenseStatus.TRIAL -> PendingAmberContainer
                            else -> RefundRedContainer
                        }
                    ) {
                        Text(
                            text = license.status.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (license.status) {
                                LicenseStatus.ACTIVE -> SuccessGreen
                                LicenseStatus.TRIAL -> PendingAmber
                                else -> RefundRed
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "License Key:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = license.licenseKey,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoundboxBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Bound Device ID:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = license.deviceHardwareId,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Pricing Tier:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "₹${license.priceInRupees} Lifetime Single Device",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Days Remaining:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${license.daysRemaining} days valid",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Activate New Key Input
                Text(
                    text = "Activate New License Key:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it.uppercase() },
                    placeholder = { Text("e.g. LVA-100-XXXX-XXXX") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = SoundboxBlue)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("license_key_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (inputKey.isNotBlank()) {
                                val ok = viewModel.activateLicense(inputKey)
                                if (ok) {
                                    inputKey = ""
                                    Toast.makeText(context, "License Activated! Bound to device hardware.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Invalid format! Key must start with LVA-100-", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter a license key or tap 'Generate Key'", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("activate_license_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoundboxBlue)
                    ) {
                        Text("Activate")
                    }

                    Button(
                        onClick = {
                            val generated = viewModel.generateNewKeyForThisDevice()
                            inputKey = generated
                            Toast.makeText(context, "Generated ₹100 key for device!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_key_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Generate Key")
                    }
                }

                adminMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoundboxBlue.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SoundboxBlue,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Merchant Management Directory
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Merchant Terminals Directory",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${merchants.size} registered merchant devices",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Button(
                        onClick = { showAddMerchantDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoundboxBlue),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Merchant", fontSize = 11.sp)
                    }
                }

                merchants.forEach { m ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = m.businessName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${m.ownerName} • ${m.phone}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (m.status) {
                                        LicenseStatus.ACTIVE -> SuccessGreenContainer
                                        LicenseStatus.TRIAL -> PendingAmberContainer
                                        else -> RefundRedContainer
                                    }
                                ) {
                                    Text(
                                        text = m.status.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (m.status) {
                                            LicenseStatus.ACTIVE -> SuccessGreen
                                            LicenseStatus.TRIAL -> PendingAmber
                                            else -> RefundRed
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Device ID: ${m.deviceId}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Text(
                                    text = "Key: ${m.licenseKey.take(12)}...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SoundboxBlue
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (m.status == LicenseStatus.ACTIVE) {
                                    TextButton(onClick = { viewModel.updateMerchantStatus(m.merchantId, LicenseStatus.REVOKED) }) {
                                        Text("Revoke", fontSize = 11.sp, color = RefundRed)
                                    }
                                } else {
                                    TextButton(onClick = { viewModel.updateMerchantStatus(m.merchantId, LicenseStatus.ACTIVE) }) {
                                        Text("Activate", fontSize = 11.sp, color = SuccessGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reset to Initial Sample Data
        Button(
            onClick = {
                viewModel.resetData()
                Toast.makeText(context, "Reset to initial sample transactions!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Sample Payment Data")
        }
    }
}

@Composable
fun AddMerchantDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, owner: String, phone: String, devId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var devId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Register New Merchant Terminal", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Generates a dedicated ₹100 license linked to the target device hardware ID.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Business / Shop Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = devId,
                    onValueChange = { devId = it },
                    label = { Text("Hardware Device ID (optional)") },
                    placeholder = { Text("LVA-DEV-XXXX-XXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name.trim(), owner.trim(), phone.trim(), devId.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoundboxBlue)
            ) {
                Text("Register & Issue Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
