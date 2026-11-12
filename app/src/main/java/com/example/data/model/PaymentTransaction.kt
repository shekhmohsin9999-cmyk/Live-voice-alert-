package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PaymentStatus {
    SUCCESS,
    PENDING,
    REFUNDED
}

enum class UpiApp(val displayName: String, val packageName: String) {
    GOOGLE_PAY("Google Pay", "com.google.android.apps.nbu.paisa.user"),
    PHONE_PE("PhonePe", "com.phonepe.app"),
    PAYTM("Paytm", "net.one97.paytm"),
    BHIM("BHIM UPI", "in.org.npci.upiapp"),
    CRED("CRED", "com.dreamplug.androidapp"),
    OTHER("UPI Bank", "other.upi");

    companion object {
        fun fromPackageName(pkg: String?): UpiApp {
            if (pkg == null) return OTHER
            return when {
                pkg.contains("google.android.apps.nbu.paisa") -> GOOGLE_PAY
                pkg.contains("phonepe") -> PHONE_PE
                pkg.contains("paytm") -> PAYTM
                pkg.contains("upiapp") -> BHIM
                pkg.contains("dreamplug") -> CRED
                else -> OTHER
            }
        }

        fun fromName(name: String): UpiApp {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}

@Entity(tableName = "payments")
data class PaymentTransaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val customerName: String,
    val upiApp: String, // Google Pay, PhonePe, Paytm, etc.
    val status: PaymentStatus = PaymentStatus.SUCCESS,
    val timestamp: Long = System.currentTimeMillis(),
    val transactionRef: String = "UPI${System.currentTimeMillis().toString().takeLast(10)}",
    val rawNotificationText: String? = null,
    val isSimulated: Boolean = false
)
