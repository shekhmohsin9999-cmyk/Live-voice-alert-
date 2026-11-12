package com.example.data.model

enum class LicenseStatus(val label: String) {
    ACTIVE("Active (सक्रिय)"),
    TRIAL("Trial (ट्रायल)"),
    EXPIRED("Expired (समाप्त)"),
    REVOKED("Revoked (रद्द)")
}

data class DeviceLicense(
    val licenseKey: String = "LVA-100-8841-K92X",
    val priceInRupees: Int = 100, // ₹100 license
    val deviceHardwareId: String = "LVA-DEV-9842-8812", // Cryptographically bound to this hardware ID
    val status: LicenseStatus = LicenseStatus.ACTIVE,
    val merchantName: String = "Sharma Kirana & General Store",
    val activatedTimestamp: Long = System.currentTimeMillis() - (15L * 24 * 60 * 60 * 1000), // 15 days ago
    val expiryTimestamp: Long = System.currentTimeMillis() + (350L * 24 * 60 * 60 * 1000), // ~1 year validity
    val planType: String = "₹100 Single Device Lifetime License",
    val authorizedByAdmin: String = "LiveVoiceAlert Admin"
) {
    val isCurrentlyValid: Boolean
        get() = (status == LicenseStatus.ACTIVE || status == LicenseStatus.TRIAL) &&
                (expiryTimestamp > System.currentTimeMillis())

    val daysRemaining: Long
        get() {
            val diff = expiryTimestamp - System.currentTimeMillis()
            return if (diff > 0) diff / (24 * 60 * 60 * 1000) else 0
        }
}

data class MerchantLicenseEntry(
    val merchantId: String,
    val businessName: String,
    val ownerName: String,
    val phone: String,
    val deviceId: String,
    val licenseKey: String,
    val status: LicenseStatus,
    val registeredDate: Long,
    val totalVolume: Double
)
