package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.repository.PaymentRepository
import com.example.service.VoiceAlertManager

class SoundboxApplication : Application() {

    lateinit var repository: PaymentRepository
        private set

    lateinit var voiceAlertManager: VoiceAlertManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = PaymentRepository(this)
        voiceAlertManager = VoiceAlertManager(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundboxChannel = NotificationChannel(
                CHANNEL_SOUNDBOX_SERVICE,
                "Live Voice Alert Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent soundbox status and background listening"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_PAYMENT_ALERTS,
                "Payment Received Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming UPI payments"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(soundboxChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_SOUNDBOX_SERVICE = "soundbox_foreground_channel"
        const val CHANNEL_PAYMENT_ALERTS = "payment_alerts_channel"

        lateinit var instance: SoundboxApplication
            private set
    }
}
