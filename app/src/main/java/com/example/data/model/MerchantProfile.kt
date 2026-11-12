package com.example.data.model

enum class VoiceLanguage(val displayName: String, val code: String, val sampleText: String) {
    HINDI("Hindi (हिंदी)", "hi", "₹500 प्राप्त हुए"),
    HINGLISH("Hinglish", "hi-IN", "Paytm par ₹500 prapt hue"),
    ENGLISH("English (India)", "en-IN", "Received 500 Rupees on Google Pay"),
    TAMIL("Tamil (தமிழ்)", "ta", "₹500 பெறப்பட்டது"),
    TELUGU("Telugu (తెలుగు)", "te", "₹500 అందింది"),
    MARATHI("Marathi (मराठी)", "mr", "₹500 प्राप्त झाले"),
    BENGALI("Bengali (বাংলা)", "bn", "₹500 পাওয়া গেছে");

    companion object {
        fun fromCode(code: String): VoiceLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
        }
    }
}

enum class AnnouncementTemplate(val id: String, val title: String, val format: String) {
    FULL_WITH_CUSTOMER("full_cust", "Payer Name & App (ग्राहक नाम सहित)", "{customer} से {app} पर ₹{amount} प्राप्त हुए"),
    APP_AND_AMOUNT("app_amt", "App & Amount (ऐप और राशि)", "{app} पर ₹{amount} प्राप्त हुए"),
    AMOUNT_ONLY("amt_only", "Amount Only (केवल राशि)", "₹{amount} प्राप्त हुए");

    fun render(amount: String, appName: String, customer: String, lang: VoiceLanguage): String {
        val cleanCust = customer.trim()
        val hasCustomer = cleanCust.isNotBlank() && cleanCust != "UPI Customer" && cleanCust != "Customer"
        return when (lang) {
            VoiceLanguage.HINDI -> when (this) {
                FULL_WITH_CUSTOMER -> if (hasCustomer) "$cleanCust से $appName पर $amount रुपये प्राप्त हुए" else "$appName पर $amount रुपये प्राप्त हुए"
                APP_AND_AMOUNT -> if (hasCustomer) "$appName पर $cleanCust से $amount रुपये प्राप्त हुए" else "$appName पर $amount रुपये प्राप्त हुए"
                AMOUNT_ONLY -> if (hasCustomer) "$cleanCust से $amount रुपये प्राप्त हुए" else "$amount रुपये प्राप्त हुए"
            }
            VoiceLanguage.HINGLISH -> when (this) {
                FULL_WITH_CUSTOMER -> if (hasCustomer) "$cleanCust se $appName par $amount rupees prapt hue" else "$appName par $amount rupees prapt hue"
                APP_AND_AMOUNT -> if (hasCustomer) "$appName par $cleanCust se $amount rupees prapt hue" else "$appName par $amount rupees prapt hue"
                AMOUNT_ONLY -> if (hasCustomer) "$cleanCust se $amount rupees prapt hue" else "Received $amount rupees"
            }
            VoiceLanguage.ENGLISH -> when (this) {
                FULL_WITH_CUSTOMER -> if (hasCustomer) "Received $amount Rupees on $appName from $cleanCust" else "Received $amount Rupees on $appName"
                APP_AND_AMOUNT -> if (hasCustomer) "Received $amount Rupees on $appName from $cleanCust" else "Received $amount Rupees on $appName"
                AMOUNT_ONLY -> if (hasCustomer) "Received $amount Rupees from $cleanCust" else "Received $amount Rupees"
            }
            VoiceLanguage.TAMIL -> if (hasCustomer) "$cleanCust இடமிருந்து $appName மூலம் $amount ரூபாய் பெறப்பட்டது" else "$appName மூலம் $amount ரூபாய் பெறப்பட்டது"
            VoiceLanguage.TELUGU -> if (hasCustomer) "$cleanCust నుండి $appName లో $amount రూపాయలు అందాయి" else "$appName లో $amount రూపాయలు అందాయి"
            VoiceLanguage.MARATHI -> if (hasCustomer) "$cleanCust कडून $appName वर $amount रुपये प्राप्त झाले" else "$appName वर $amount रुपये प्राप्त झाले"
            VoiceLanguage.BENGALI -> if (hasCustomer) "$cleanCust থেকে $appName এ $amount টাকা পাওয়া গেছে" else "$appName এ $amount টাকা পাওয়া গেছে"
        }
    }
}

data class MerchantProfile(
    val businessName: String = "Sharma Kirana & General Store",
    val merchantVpa: String = "sharmakirana@okhdfcbank",
    val merchantPhone: String = "+91 98765 43210",
    val deviceHardwareId: String = "LVA-DEV-9842-8812",
    val voiceAlertsEnabled: Boolean = true,
    val selectedLanguage: VoiceLanguage = VoiceLanguage.HINDI,
    val announcementTemplate: AnnouncementTemplate = AnnouncementTemplate.FULL_WITH_CUSTOMER,
    val volumeLevel: Int = 100, // 0 - 100
    val speechRate: Float = 1.0f, // 0.7 - 1.5
    val speechPitch: Float = 1.0f, // 0.8 - 1.3
    val repeatCount: Int = 1, // 1 or 2
    val vibrateOnPayment: Boolean = true
)
