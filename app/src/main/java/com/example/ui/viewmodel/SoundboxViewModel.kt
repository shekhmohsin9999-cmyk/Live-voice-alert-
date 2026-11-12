package com.example.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SoundboxApplication
import com.example.data.model.AnnouncementTemplate
import com.example.data.model.DeviceLicense
import com.example.data.model.LicenseStatus
import com.example.data.model.MerchantLicenseEntry
import com.example.data.model.MerchantProfile
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.UpiApp
import com.example.data.model.VoiceLanguage
import com.example.data.repository.PaymentRepository
import com.example.service.SoundboxNotificationService
import com.example.service.VoiceAlertManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeFilter(val label: String) {
    TODAY("Today"),
    LAST_WEEK("Last Week"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom Date")
}

data class FilterCriteria(
    val timeFilter: TimeFilter = TimeFilter.TODAY,
    val customRange: Pair<Long, Long>? = null,
    val query: String = "",
    val appFilter: String? = null,
    val statusFilter: PaymentStatus? = null
)

data class AppStats(
    val appName: String,
    val count: Int,
    val totalAmount: Double,
    val percentage: Float
)

data class DashboardStats(
    val todayAmount: Double = 0.0,
    val todayCount: Int = 0,
    val totalAmount: Double = 0.0,
    val totalCount: Int = 0,
    val averageTicket: Double = 0.0,
    val successRate: Float = 100f,
    val appDistribution: List<AppStats> = emptyList()
)

class SoundboxViewModel(
    private val repository: PaymentRepository,
    private val voiceAlertManager: VoiceAlertManager
) : ViewModel() {

    val merchantProfile: StateFlow<MerchantProfile> = repository.merchantProfile
    val deviceLicense: StateFlow<DeviceLicense> = repository.deviceLicense
    val registeredMerchants: StateFlow<List<MerchantLicenseEntry>> = repository.registeredMerchants

    val allPayments = repository.getAllPayments()

    // Recent payments stream for Dashboard (unfiltered, always shows newest on top)
    val recentPayments: StateFlow<List<PaymentTransaction>> = allPayments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filter states
    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.TODAY)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAppFilter = MutableStateFlow<String?>(null)
    val selectedAppFilter: StateFlow<String?> = _selectedAppFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<PaymentStatus?>(null)
    val selectedStatusFilter: StateFlow<PaymentStatus?> = _selectedStatusFilter.asStateFlow()

    private val _isSpeakingNow = MutableStateFlow(false)
    val isSpeakingNow: StateFlow<Boolean> = _isSpeakingNow.asStateFlow()

    private val _currentAnnouncement = MutableStateFlow<String?>(null)
    val currentAnnouncement: StateFlow<String?> = _currentAnnouncement.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    private val filterCriteria = combine(
        _selectedTimeFilter,
        _customDateRange,
        _searchQuery,
        _selectedAppFilter,
        _selectedStatusFilter
    ) { timeFilter, customRange, query, appFilter, statusFilter ->
        FilterCriteria(timeFilter, customRange, query, appFilter, statusFilter)
    }

    // Combined filtered payments for History Screen
    val filteredPayments: StateFlow<List<PaymentTransaction>> = combine(
        allPayments,
        filterCriteria
    ) { payments: List<PaymentTransaction>, filter: FilterCriteria ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Time range filter
        val timeFiltered = payments.filter { p ->
            when (filter.timeFilter) {
                TimeFilter.TODAY -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    p.timestamp >= cal.timeInMillis
                }
                TimeFilter.LAST_WEEK -> {
                    p.timestamp >= (now - 7L * 24 * 60 * 60 * 1000)
                }
                TimeFilter.LAST_MONTH -> {
                    p.timestamp >= (now - 30L * 24 * 60 * 60 * 1000)
                }
                TimeFilter.CUSTOM -> {
                    val range = filter.customRange
                    if (range != null) {
                        p.timestamp in range.first..range.second
                    } else true
                }
            }
        }

        // 2. Search query filter
        val searchFiltered = if (filter.query.isBlank()) {
            timeFiltered
        } else {
            timeFiltered.filter {
                it.customerName.contains(filter.query, ignoreCase = true) ||
                it.transactionRef.contains(filter.query, ignoreCase = true) ||
                it.amount.toString().contains(filter.query) ||
                it.upiApp.contains(filter.query, ignoreCase = true)
            }
        }

        // 3. App filter
        val appFiltered = if (filter.appFilter == null) {
            searchFiltered
        } else {
            searchFiltered.filter { it.upiApp.equals(filter.appFilter, ignoreCase = true) }
        }

        // 4. Status filter
        if (filter.statusFilter == null) {
            appFiltered
        } else {
            appFiltered.filter { it.status == filter.statusFilter }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dashboard Statistics
    val dashboardStats: StateFlow<DashboardStats> = combine(
        allPayments,
        merchantProfile
    ) { payments: List<PaymentTransaction>, _ ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        val todayPayments = payments.filter { it.timestamp >= startOfToday && it.status == PaymentStatus.SUCCESS }
        val allSuccessPayments = payments.filter { it.status == PaymentStatus.SUCCESS }

        val todayAmt = todayPayments.sumOf { it.amount }
        val totalAmt = allSuccessPayments.sumOf { it.amount }
        val avgTicket = if (allSuccessPayments.isNotEmpty()) totalAmt / allSuccessPayments.size else 0.0

        val totalCount = payments.size
        val successCount = allSuccessPayments.size
        val successRate = if (totalCount > 0) (successCount.toFloat() / totalCount.toFloat()) * 100f else 100f

        // UPI app distribution
        val appCounts = allSuccessPayments.groupBy { it.upiApp }
        val appStatsList = appCounts.map { (app, txns) ->
            val count = txns.size
            val sum = txns.sumOf { it.amount }
            val pct = if (totalAmt > 0) (sum / totalAmt).toFloat() * 100f else 0f
            AppStats(appName = app, count = count, totalAmount = sum, percentage = pct)
        }.sortedByDescending { it.totalAmount }

        DashboardStats(
            todayAmount = todayAmt,
            todayCount = todayPayments.size,
            totalAmount = totalAmt,
            totalCount = allSuccessPayments.size,
            averageTicket = avgTicket,
            successRate = successRate,
            appDistribution = appStatsList
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardStats())

    fun setTimeFilter(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = Pair(start, end)
        _selectedTimeFilter.value = TimeFilter.CUSTOM
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAppFilter(app: String?) {
        _selectedAppFilter.value = app
    }

    fun setStatusFilter(status: PaymentStatus?) {
        _selectedStatusFilter.value = status
    }

    fun toggleVoiceAlerts(enabled: Boolean) {
        repository.toggleVoiceAlerts(enabled)
    }

    fun setVoiceLanguage(language: VoiceLanguage) {
        repository.updateLanguage(language)
    }

    fun setAnnouncementTemplate(template: AnnouncementTemplate) {
        val current = merchantProfile.value
        repository.updateMerchantProfile(current.copy(announcementTemplate = template))
    }

    fun setVolume(volume: Int) {
        repository.updateVolume(volume)
    }

    fun setSpeechRate(rate: Float) {
        val current = merchantProfile.value
        repository.updateMerchantProfile(current.copy(speechRate = rate))
    }

    fun setSpeechPitch(pitch: Float) {
        val current = merchantProfile.value
        repository.updateMerchantProfile(current.copy(speechPitch = pitch))
    }

    fun setRepeatCount(count: Int) {
        val current = merchantProfile.value
        repository.updateMerchantProfile(current.copy(repeatCount = count))
    }

    fun setVibrateOnPayment(enabled: Boolean) {
        val current = merchantProfile.value
        repository.updateMerchantProfile(current.copy(vibrateOnPayment = enabled))
    }

    fun updateMerchantProfile(name: String, vpa: String, phone: String) {
        val current = merchantProfile.value
        repository.updateMerchantProfile(
            current.copy(
                businessName = name.trim(),
                merchantVpa = vpa.trim(),
                merchantPhone = phone.trim()
            )
        )
    }

    // Voice announcement actions
    fun playTestVoice(amount: Double = 500.0, upiApp: String = "Google Pay", customer: String = "Rahul Sharma") {
        val profile = merchantProfile.value
        val formattedAmount = if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
        val text = profile.announcementTemplate.render(
            amount = formattedAmount,
            appName = upiApp,
            customer = customer,
            lang = profile.selectedLanguage
        )
        _currentAnnouncement.value = text
        _isSpeakingNow.value = true
        voiceAlertManager.announcePayment(
            amount = amount,
            upiApp = upiApp,
            customerName = customer,
            profile = profile,
            forceAnnounce = true, // Manual test button always announces
            onComplete = {
                _isSpeakingNow.value = false
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2800L)
                    if (_currentAnnouncement.value == text) {
                        _currentAnnouncement.value = null
                    }
                }
            }
        )
    }

    fun reAnnounce(payment: PaymentTransaction) {
        val profile = merchantProfile.value
        val formattedAmount = if (payment.amount % 1.0 == 0.0) payment.amount.toInt().toString() else payment.amount.toString()
        val text = profile.announcementTemplate.render(
            amount = formattedAmount,
            appName = payment.upiApp,
            customer = payment.customerName,
            lang = profile.selectedLanguage
        )
        _currentAnnouncement.value = text
        _isSpeakingNow.value = true
        voiceAlertManager.announcePayment(
            amount = payment.amount,
            upiApp = payment.upiApp,
            customerName = payment.customerName,
            profile = profile,
            forceAnnounce = true,
            onComplete = {
                _isSpeakingNow.value = false
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2800L)
                    if (_currentAnnouncement.value == text) {
                        _currentAnnouncement.value = null
                    }
                }
            }
        )
    }

    fun simulateIncomingPayment(
        amount: Double,
        customerName: String,
        upiApp: UpiApp
    ) {
        viewModelScope.launch {
            val payment = PaymentTransaction(
                amount = amount,
                customerName = customerName.ifBlank { "UPI Customer" },
                upiApp = upiApp.displayName,
                status = PaymentStatus.SUCCESS,
                timestamp = System.currentTimeMillis(),
                transactionRef = "UPI" + System.currentTimeMillis().toString().takeLast(10),
                rawNotificationText = "${upiApp.displayName}: Received payment of ₹$amount from $customerName",
                isSimulated = true
            )
            repository.insertPayment(payment)

            val profile = merchantProfile.value
            val formattedAmount = if (payment.amount % 1.0 == 0.0) payment.amount.toInt().toString() else payment.amount.toString()
            val text = profile.announcementTemplate.render(
                amount = formattedAmount,
                appName = payment.upiApp,
                customer = payment.customerName,
                lang = profile.selectedLanguage
            )
            _currentAnnouncement.value = text
            _isSpeakingNow.value = true
            voiceAlertManager.announcePayment(
                amount = payment.amount,
                upiApp = payment.upiApp,
                customerName = payment.customerName,
                profile = profile,
                forceAnnounce = true,
                onComplete = {
                    _isSpeakingNow.value = false
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2800L)
                        if (_currentAnnouncement.value == text) {
                            _currentAnnouncement.value = null
                        }
                    }
                }
            )
        }
    }

    // ₹100 Single-Device License actions
    fun activateLicense(key: String): Boolean {
        val success = repository.activateLicense(key)
        _adminMessage.value = if (success) {
            "License activated successfully! Bound to device hardware."
        } else {
            "Invalid License Key format. Example: LVA-100-XXXX-XXXX"
        }
        return success
    }

    fun generateNewKeyForThisDevice(): String {
        val key = repository.generateLicenseKeyForDevice(repository.getDeviceHardwareId())
        return key
    }

    fun clearAdminMessage() {
        _adminMessage.value = null
    }

    fun updateMerchantStatus(merchantId: String, newStatus: LicenseStatus) {
        repository.updateMerchantLicenseStatus(merchantId, newStatus)
    }

    fun addMerchant(name: String, owner: String, phone: String, devId: String) {
        val generatedKey = repository.generateLicenseKeyForDevice(devId)
        val entry = MerchantLicenseEntry(
            merchantId = "M-${(100..999).random()}",
            businessName = name,
            ownerName = owner,
            phone = phone,
            deviceId = devId.ifBlank { "LVA-DEV-${(1000..9999).random()}" },
            licenseKey = generatedKey,
            status = LicenseStatus.ACTIVE,
            registeredDate = System.currentTimeMillis(),
            totalVolume = 0.0
        )
        repository.addMerchantToDirectory(entry)
    }

    fun resetData() {
        viewModelScope.launch {
            repository.resetToSampleData()
        }
    }

    // Permission and System helpers
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        val myComponent = ComponentName(context, SoundboxNotificationService::class.java).flattenToString()
        return enabledListeners != null && enabledListeners.contains(myComponent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext as SoundboxApplication
                    return SoundboxViewModel(app.repository, app.voiceAlertManager) as T
                }
            }
        }
    }
}
