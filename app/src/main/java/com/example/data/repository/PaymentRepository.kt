package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.example.data.local.PaymentDao
import com.example.data.local.SoundboxDatabase
import com.example.data.model.AnnouncementTemplate
import com.example.data.model.DeviceLicense
import com.example.data.model.LicenseStatus
import com.example.data.model.MerchantLicenseEntry
import com.example.data.model.MerchantProfile
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.UpiApp
import com.example.data.model.VoiceLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class PaymentRepository(
    private val context: Context,
    private val database: SoundboxDatabase = SoundboxDatabase.getDatabase(context)
) {
    private val paymentDao: PaymentDao = database.paymentDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("soundbox_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _merchantProfile = MutableStateFlow(loadMerchantProfile())
    val merchantProfile: StateFlow<MerchantProfile> = _merchantProfile.asStateFlow()

    private val _deviceLicense = MutableStateFlow(loadDeviceLicense())
    val deviceLicense: StateFlow<DeviceLicense> = _deviceLicense.asStateFlow()

    private val _registeredMerchants = MutableStateFlow(loadMerchantDirectory())
    val registeredMerchants: StateFlow<List<MerchantLicenseEntry>> = _registeredMerchants.asStateFlow()

    init {
        repositoryScope.launch {
            if (paymentDao.count() == 0) {
                seedInitialSamplePayments()
            }
        }
    }

    fun getAllPayments(): Flow<List<PaymentTransaction>> = paymentDao.getAllPayments()

    fun getPaymentsBetween(startTime: Long, endTime: Long): Flow<List<PaymentTransaction>> {
        return paymentDao.getPaymentsBetween(startTime, endTime)
    }

    suspend fun insertPayment(payment: PaymentTransaction) {
        paymentDao.insert(payment)
    }

    suspend fun clearHistory() {
        paymentDao.clearAll()
    }

    suspend fun resetToSampleData() {
        paymentDao.clearAll()
        seedInitialSamplePayments()
    }

    fun updateMerchantProfile(newProfile: MerchantProfile) {
        _merchantProfile.value = newProfile
        saveMerchantProfile(newProfile)
    }

    fun toggleVoiceAlerts(enabled: Boolean) {
        val updated = _merchantProfile.value.copy(voiceAlertsEnabled = enabled)
        updateMerchantProfile(updated)
    }

    fun updateLanguage(language: VoiceLanguage) {
        val updated = _merchantProfile.value.copy(selectedLanguage = language)
        updateMerchantProfile(updated)
    }

    fun updateVolume(volume: Int) {
        val updated = _merchantProfile.value.copy(volumeLevel = volume)
        updateMerchantProfile(updated)
    }

    // ==========================================
    // ₹100 Single-Device License Management
    // ==========================================

    fun activateLicense(key: String): Boolean {
        val currentDevice = getDeviceHardwareId()
        val trimmed = key.trim().uppercase()

        // Validation rule: Key must match format LVA-100-XXXX-XXXX
        val isValidFormat = trimmed.startsWith("LVA-100-") && trimmed.length >= 14
        if (!isValidFormat) return false

        val updated = _deviceLicense.value.copy(
            licenseKey = trimmed,
            status = LicenseStatus.ACTIVE,
            deviceHardwareId = currentDevice,
            activatedTimestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
        )
        _deviceLicense.value = updated
        saveDeviceLicense(updated)
        return true
    }

    fun generateLicenseKeyForDevice(targetDeviceId: String): String {
        val randomPart = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val chunk1 = randomPart.substring(0, 4)
        val chunk2 = randomPart.substring(4, 8)
        return "LVA-100-$chunk1-$chunk2"
    }

    fun updateMerchantLicenseStatus(merchantId: String, newStatus: LicenseStatus) {
        val list = _registeredMerchants.value.toMutableList()
        val idx = list.indexOfFirst { it.merchantId == merchantId }
        if (idx != -1) {
            list[idx] = list[idx].copy(status = newStatus)
            _registeredMerchants.value = list
        }
    }

    fun addMerchantToDirectory(entry: MerchantLicenseEntry) {
        val list = _registeredMerchants.value.toMutableList()
        list.add(0, entry)
        _registeredMerchants.value = list
    }

    fun getDeviceHardwareId(): String {
        val existing = prefs.getString("device_hardware_id", null)
        if (!existing.isNullOrBlank()) return existing

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }

        val generated = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            "LVA-DEV-${androidId.takeLast(8).uppercase()}"
        } else {
            "LVA-DEV-${UUID.randomUUID().toString().take(8).uppercase()}"
        }

        prefs.edit().putString("device_hardware_id", generated).apply()
        return generated
    }

    // ==========================================
    // SharedPreferences Persistence
    // ==========================================

    private fun loadMerchantProfile(): MerchantProfile {
        val deviceId = getDeviceHardwareId()
        return MerchantProfile(
            businessName = prefs.getString("biz_name", "Sharma Kirana & General Store") ?: "Sharma Kirana & General Store",
            merchantVpa = prefs.getString("biz_vpa", "sharmakirana@okhdfcbank") ?: "sharmakirana@okhdfcbank",
            merchantPhone = prefs.getString("biz_phone", "+91 98765 43210") ?: "+91 98765 43210",
            deviceHardwareId = deviceId,
            voiceAlertsEnabled = prefs.getBoolean("voice_enabled", true),
            selectedLanguage = VoiceLanguage.fromCode(prefs.getString("voice_lang", "hi") ?: "hi"),
            announcementTemplate = AnnouncementTemplate.entries.firstOrNull {
                it.id == prefs.getString("voice_template", "full_cust")
            } ?: AnnouncementTemplate.FULL_WITH_CUSTOMER,
            volumeLevel = prefs.getInt("voice_volume", 100),
            speechRate = prefs.getFloat("voice_speed", 1.0f),
            speechPitch = prefs.getFloat("voice_pitch", 1.0f),
            repeatCount = prefs.getInt("voice_repeat", 1),
            vibrateOnPayment = prefs.getBoolean("voice_vibrate", true)
        )
    }

    private fun saveMerchantProfile(profile: MerchantProfile) {
        prefs.edit().apply {
            putString("biz_name", profile.businessName)
            putString("biz_vpa", profile.merchantVpa)
            putString("biz_phone", profile.merchantPhone)
            putBoolean("voice_enabled", profile.voiceAlertsEnabled)
            putString("voice_lang", profile.selectedLanguage.code)
            putString("voice_template", profile.announcementTemplate.id)
            putInt("voice_volume", profile.volumeLevel)
            putFloat("voice_speed", profile.speechRate)
            putFloat("voice_pitch", profile.speechPitch)
            putInt("voice_repeat", profile.repeatCount)
            putBoolean("voice_vibrate", profile.vibrateOnPayment)
            apply()
        }
    }

    private fun loadDeviceLicense(): DeviceLicense {
        val deviceId = getDeviceHardwareId()
        val key = prefs.getString("license_key", "LVA-100-8841-K92X") ?: "LVA-100-8841-K92X"
        val statusStr = prefs.getString("license_status", LicenseStatus.ACTIVE.name) ?: LicenseStatus.ACTIVE.name
        val status = try { LicenseStatus.valueOf(statusStr) } catch (e: Exception) { LicenseStatus.ACTIVE }
        val exp = prefs.getLong("license_exp", System.currentTimeMillis() + (350L * 24 * 60 * 60 * 1000))

        return DeviceLicense(
            licenseKey = key,
            priceInRupees = 100,
            deviceHardwareId = deviceId,
            status = status,
            merchantName = _merchantProfile.value.businessName,
            expiryTimestamp = exp
        )
    }

    private fun saveDeviceLicense(license: DeviceLicense) {
        prefs.edit().apply {
            putString("license_key", license.licenseKey)
            putString("license_status", license.status.name)
            putLong("license_exp", license.expiryTimestamp)
            apply()
        }
    }

    private fun loadMerchantDirectory(): List<MerchantLicenseEntry> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        return listOf(
            MerchantLicenseEntry(
                merchantId = "M-101",
                businessName = "Sharma Kirana & General Store",
                ownerName = "Ramesh Sharma",
                phone = "+91 98765 43210",
                deviceId = getDeviceHardwareId(),
                licenseKey = "LVA-100-8841-K92X",
                status = LicenseStatus.ACTIVE,
                registeredDate = now - (30 * day),
                totalVolume = 142800.0
            ),
            MerchantLicenseEntry(
                merchantId = "M-102",
                businessName = "Gupta Sweets & Bakers",
                ownerName = "Sunil Gupta",
                phone = "+91 98112 34567",
                deviceId = "LVA-DEV-4321-9988",
                licenseKey = "LVA-100-5521-A190",
                status = LicenseStatus.ACTIVE,
                registeredDate = now - (45 * day),
                totalVolume = 238500.0
            ),
            MerchantLicenseEntry(
                merchantId = "M-103",
                businessName = "Balaji Mobile & Electronics",
                ownerName = "Mahesh Patel",
                phone = "+91 97234 56789",
                deviceId = "LVA-DEV-7762-2301",
                licenseKey = "LVA-100-9920-F711",
                status = LicenseStatus.ACTIVE,
                registeredDate = now - (60 * day),
                totalVolume = 389200.0
            ),
            MerchantLicenseEntry(
                merchantId = "M-104",
                businessName = "Apna Medical Store",
                ownerName = "Dr. Ankit Verma",
                phone = "+91 98450 11223",
                deviceId = "LVA-DEV-1192-8876",
                licenseKey = "LVA-100-3329-P450",
                status = LicenseStatus.TRIAL,
                registeredDate = now - (3 * day),
                totalVolume = 18400.0
            ),
            MerchantLicenseEntry(
                merchantId = "M-105",
                businessName = "Chai Point & Snacks",
                ownerName = "Vikas Yadav",
                phone = "+91 99345 88990",
                deviceId = "LVA-DEV-9021-3312",
                licenseKey = "LVA-100-0012-E882",
                status = LicenseStatus.EXPIRED,
                registeredDate = now - (400 * day),
                totalVolume = 78200.0
            )
        )
    }

    private suspend fun seedInitialSamplePayments() {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val day = 24 * hour

        val samples = listOf(
            PaymentTransaction(
                amount = 500.0,
                customerName = "Rahul Sharma",
                upiApp = UpiApp.PAYTM.displayName,
                timestamp = now - (15 * 60 * 1000L), // 15 mins ago
                transactionRef = "UPI4892184910",
                rawNotificationText = "Paytm for Business: Received ₹500 from Rahul Sharma",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 1200.0,
                customerName = "Pooja Patel",
                upiApp = UpiApp.GOOGLE_PAY.displayName,
                timestamp = now - (45 * 60 * 1000L), // 45 mins ago
                transactionRef = "UPI4892183201",
                rawNotificationText = "Google Pay: Pooja Patel paid you ₹1,200",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 350.0,
                customerName = "Amit Verma",
                upiApp = UpiApp.PHONE_PE.displayName,
                timestamp = now - (2 * hour),
                transactionRef = "UPI4892181140",
                rawNotificationText = "PhonePe: You have received ₹350 from Amit Verma",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 2500.0,
                customerName = "Vikram Singh",
                upiApp = UpiApp.GOOGLE_PAY.displayName,
                timestamp = now - (4 * hour),
                transactionRef = "UPI4892178902",
                rawNotificationText = "Google Pay: Received ₹2,500 from Vikram Singh",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 150.0,
                customerName = "Suresh Yadav",
                upiApp = UpiApp.PAYTM.displayName,
                timestamp = now - (5 * hour),
                transactionRef = "UPI4892176430",
                rawNotificationText = "Paytm: ₹150 received from Suresh Yadav",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 750.0,
                customerName = "Anjali Gupta",
                upiApp = UpiApp.PHONE_PE.displayName,
                timestamp = now - (7 * hour),
                transactionRef = "UPI4892172291",
                rawNotificationText = "PhonePe: Received ₹750 from Anjali Gupta",
                status = PaymentStatus.SUCCESS
            ),
            // Yesterday & Last Week
            PaymentTransaction(
                amount = 3400.0,
                customerName = "Kunal Mehra",
                upiApp = UpiApp.BHIM.displayName,
                timestamp = now - (1 * day) - (2 * hour),
                transactionRef = "UPI4892091102",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 890.0,
                customerName = "Deepak Joshi",
                upiApp = UpiApp.PAYTM.displayName,
                timestamp = now - (1 * day) - (5 * hour),
                transactionRef = "UPI4892083921",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 450.0,
                customerName = "Neha Agarwal",
                upiApp = UpiApp.GOOGLE_PAY.displayName,
                timestamp = now - (2 * day),
                transactionRef = "UPI4891992019",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 1800.0,
                customerName = "Manoj Tiwari",
                upiApp = UpiApp.PHONE_PE.displayName,
                timestamp = now - (3 * day),
                transactionRef = "UPI4891823901",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 620.0,
                customerName = "Priya Sen",
                upiApp = UpiApp.CRED.displayName,
                timestamp = now - (5 * day),
                transactionRef = "UPI4891629402",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 4200.0,
                customerName = "Rajesh Nair",
                upiApp = UpiApp.GOOGLE_PAY.displayName,
                timestamp = now - (6 * day),
                transactionRef = "UPI4891510291",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 950.0,
                customerName = "Sneha Kulkarni",
                upiApp = UpiApp.PAYTM.displayName,
                timestamp = now - (12 * day),
                transactionRef = "UPI4890881920",
                status = PaymentStatus.SUCCESS
            ),
            PaymentTransaction(
                amount = 5100.0,
                customerName = "Alok Dixit",
                upiApp = UpiApp.PHONE_PE.displayName,
                timestamp = now - (20 * day),
                transactionRef = "UPI4890123984",
                status = PaymentStatus.SUCCESS
            )
        )

        paymentDao.insertAll(samples)
    }
}
