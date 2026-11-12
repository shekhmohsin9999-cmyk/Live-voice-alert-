package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SoundboxBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CustomDateRangeDialog(
    onDismiss: () -> Unit,
    onApplyRange: (start: Long, end: Long) -> Unit
) {
    // Select days range: e.g. Last 3 Days, Last 14 Days, Last 60 Days, Last 90 Days
    var selectedDaysIndex by remember { mutableIntStateOf(14) }
    val dayOptions = listOf(3, 7, 14, 30, 60, 90)

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val now = System.currentTimeMillis()
    val startTime = now - (selectedDaysIndex * 24L * 60 * 60 * 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = SoundboxBlue
                )
                Text(
                    text = "Select Custom Date Filter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Choose custom time interval to analyze transaction history and collection volume:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Date Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoundboxBlue.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Filtered Range:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dateFormat.format(startTime)}  →  ${dateFormat.format(now)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoundboxBlue
                        )
                        Text(
                            text = "Past $selectedDaysIndex days",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Quick Presets:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayOptions.chunked(3).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { days ->
                                val isSelected = selectedDaysIndex == days
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDaysIndex = days },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) SoundboxBlue else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "$days Days",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyRange(startTime, now)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoundboxBlue)
            ) {
                Text("Apply Range")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
