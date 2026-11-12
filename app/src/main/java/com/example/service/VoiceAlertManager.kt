package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.MerchantProfile
import com.example.data.model.VoiceLanguage
import java.text.DecimalFormat
import java.util.Locale
import java.util.UUID

class VoiceAlertManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingUtterances = mutableListOf<() -> Unit>()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupTtsEngine(VoiceLanguage.HINDI, 1.0f, 1.0f)
            Log.d(TAG, "TextToSpeech initialized successfully")

            // Drain any pending speech requests
            synchronized(pendingUtterances) {
                while (pendingUtterances.isNotEmpty()) {
                    val action = pendingUtterances.removeAt(0)
                    mainHandler.post { action() }
                }
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with code: $status")
            isInitialized = false
            synchronized(pendingUtterances) {
                while (pendingUtterances.isNotEmpty()) {
                    val action = pendingUtterances.removeAt(0)
                    mainHandler.post { action() }
                }
            }
        }
    }

    fun setupTtsEngine(language: VoiceLanguage, speechRate: Float, pitch: Float) {
        val engine = tts ?: return
        try {
            val locale = when (language) {
                VoiceLanguage.HINDI -> Locale("hi", "IN")
                VoiceLanguage.HINGLISH -> Locale("hi", "IN")
                VoiceLanguage.ENGLISH -> Locale("en", "IN")
                VoiceLanguage.TAMIL -> Locale("ta", "IN")
                VoiceLanguage.TELUGU -> Locale("te", "IN")
                VoiceLanguage.MARATHI -> Locale("mr", "IN")
                VoiceLanguage.BENGALI -> Locale("bn", "IN")
            }

            var result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Try language-only Hindi
                result = engine.setLanguage(Locale("hi"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to English or default
                    val enResult = engine.setLanguage(Locale.ENGLISH)
                    if (enResult == TextToSpeech.LANG_MISSING_DATA || enResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.getDefault())
                    }
                }
            }

            engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
            engine.setPitch(pitch.coerceIn(0.5f, 2.0f))

            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed configuring TTS engine", e)
        }
    }

    /**
     * Announces a received UPI payment through speaker with wake lock and optional vibration.
     * forceAnnounce: if true, speaks regardless of profile.voiceAlertsEnabled (used for Test button).
     */
    fun announcePayment(
        amount: Double,
        upiApp: String,
        customerName: String,
        profile: MerchantProfile,
        forceAnnounce: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        if (!profile.voiceAlertsEnabled && !forceAnnounce) {
            Log.d(TAG, "Voice alert skipped because voice alerts are toggled OFF")
            onComplete?.invoke()
            return
        }

        // 1. Wake the CPU so announcement speaks clearly even when screen is locked
        val wakeLock = try {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "LiveVoiceAlert::PaymentAnnouncementWakeLock"
            )?.apply {
                acquire(5000L) // Safe 5-second timeout
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire wake lock", e)
            null
        }

        // Guaranteed safety completion wrapper - will ALWAYS fire within 2.5s
        var isFinished = false
        val safeDone: () -> Unit = {
            if (!isFinished) {
                isFinished = true
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing wake lock", e)
                }
                mainHandler.post {
                    onComplete?.invoke()
                }
            }
        }

        // Failsafe watchdog timer: ensure onComplete is invoked even if TTS hangs, fails or is uninstalled
        mainHandler.postDelayed({
            safeDone()
        }, 2500L)

        // 2. Play audible soundbox chime tone
        playChime()

        // 3. Adjust volume to configured level
        try {
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = ((profile.volumeLevel / 100f) * maxVol).toInt().coerceIn(1, maxVol)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not adjust audio volume", e)
        }

        // 4. Trigger haptic vibration if enabled
        if (profile.vibrateOnPayment) {
            triggerVibration()
        }

        // 5. Construct speech text
        val formattedAmount = if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }

        val speechText = profile.announcementTemplate.render(
            amount = formattedAmount,
            appName = upiApp,
            customer = customerName,
            lang = profile.selectedLanguage
        )

        speak(
            text = speechText,
            repeatCount = profile.repeatCount,
            profile = profile,
            onDone = {
                safeDone()
            }
        )
    }

    fun playChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            mainHandler.postDelayed({
                try {
                    toneGen.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }, 600L)
        } catch (e: Exception) {
            Log.w(TAG, "Could not play chime tone", e)
        }
    }

    private fun speak(
        text: String,
        repeatCount: Int,
        profile: MerchantProfile,
        onDone: (() -> Unit)?
    ) {
        if (!isInitialized || tts == null) {
            Log.d(TAG, "TTS not ready, queuing latest utterance: $text")
            synchronized(pendingUtterances) {
                pendingUtterances.clear()
                pendingUtterances.add {
                    speak(text, repeatCount, profile, onDone)
                }
            }
            if (tts == null) {
                initializeTts()
            }
            // If initialization takes longer than 1200ms, trigger onDone so UI doesn't hang
            mainHandler.postDelayed({
                if (!isInitialized) {
                    onDone?.invoke()
                }
            }, 1200L)
            return
        }

        val engine = tts ?: return
        setupTtsEngine(profile.selectedLanguage, profile.speechRate, profile.speechPitch)

        val fullText = if (repeatCount > 1) {
            "$text ... $text"
        } else {
            text
        }

        val utteranceId = UUID.randomUUID().toString()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Speech started: $fullText")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Speech completed successfully")
                mainHandler.post { onDone?.invoke() }
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Speech error on utterance: $utteranceId")
                mainHandler.post { onDone?.invoke() }
            }
        })

        engine.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "VoiceAlertManager"
    }
}
