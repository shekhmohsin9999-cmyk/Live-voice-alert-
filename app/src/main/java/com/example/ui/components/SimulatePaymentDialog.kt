package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UpiApp
import com.example.ui.theme.BrandBhim
import com.example.ui.theme.BrandGPay
import com.example.ui.theme.BrandPaytm
import com.example.ui.theme.BrandPhonePe
import com.example.ui.theme.SoundboxBlue
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulatePaymentDialog(
    onDismiss: () -> Unit,
    onSimulate: (amount: Double, customer: String, app: UpiApp) -> Unit
) {
    var amountText by remember { mutableStateOf("500") }
    var customerName by remember { mutableStateOf("Rohan Gupta") }
    var selectedApp by remember { mutableStateOf(UpiApp.PAYTM) }

    val presetAmounts = listOf("100", "250", "500", "1200", "2500")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = SoundboxBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Simulate UPI Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Test incoming payment alerts from GPay, PhonePe or Paytm. This triggers a real Hindi voice announcement (\"₹$amountText प्राप्त हुए\") and logs to history.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // UPI App Selector
                Text(
                    text = "Select UPI Application:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val apps = listOf(UpiApp.PAYTM, UpiApp.GOOGLE_PAY, UpiApp.PHONE_PE, UpiApp.BHIM)
                    apps.forEach { app ->
                        val isSelected = selectedApp == app
                        val appColor = getUpiAppColor(app.displayName)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) appColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedApp = app },
                            color = if (isSelected) appColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(appColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.displayName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) appColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Payment Amount (₹)") },
                    leadingIcon = {
                        Text(
                            text = "₹",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SuccessGreen,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_amount_input")
                )

                // Quick preset amounts
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (amountText == preset) SoundboxBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (amountText == preset) {
                                androidx.compose.foundation.BorderStroke(1.dp, SoundboxBlue)
                            } else null,
                            modifier = Modifier.clickable { amountText = preset }
                        ) {
                            Text(
                                text = "₹$preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (amountText == preset) SoundboxBlue else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Customer Name Input
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name / Payer") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_customer_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 500.0
                    onSimulate(amt, customerName.trim(), selectedApp)
                    onDismiss()
                },
                modifier = Modifier.testTag("simulate_confirm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SoundboxBlue)
            ) {
                Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate & Speak")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
