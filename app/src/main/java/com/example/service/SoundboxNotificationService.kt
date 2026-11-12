package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SoundboxApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoundboxNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processedKeys = LinkedHashSet<String>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SoundboxNotificationService started")
        isRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SoundboxNotificationService destroyed")
        isRunning = false
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener connected successfully! Soundbox is listening.")
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification Listener disconnected")
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        try {
            val key = "${sbn.packageName}_${sbn.id}_${sbn.postTime}"
            if (processedKeys.contains(key)) return
            if (processedKeys.size > 200) {
                processedKeys.clear()
            }
            processedKeys.add(key)

            val parsed = UpiNotificationParser.parse(sbn) ?: return

            Log.i(TAG, "Detected UPI Payment: ₹${parsed.amount} from ${parsed.customerName} via ${parsed.upiApp.displayName}")

            val app = application as? SoundboxApplication ?: SoundboxApplication.instance
            val repository = app.repository
            val voiceManager = app.voiceAlertManager
            val profile = repository.merchantProfile.value

            val transaction = UpiNotificationParser.toTransaction(parsed, isSimulated = false)

            serviceScope.launch {
                repository.insertPayment(transaction)
            }

            // Trigger voice announcement
            voiceManager.announcePayment(
                amount = parsed.amount,
                upiApp = parsed.upiApp.displayName,
                customerName = parsed.customerName,
                profile = profile
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed on dismiss
    }

    companion object {
        private const val TAG = "SoundboxNotifService"
        var isConnected: Boolean = false
            private set
        var isRunning: Boolean = false
            private set
    }
}
