package com.example.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.UpiApp
import java.util.regex.Pattern

data class ParsedUpiPayment(
    val amount: Double,
    val customerName: String,
    val upiApp: UpiApp,
    val referenceId: String,
    val rawText: String
)

object UpiNotificationParser {

    private const val TAG = "UpiNotificationParser"

    // Amount regex patterns matching ₹500, Rs. 500, Rs 1,500.00, INR 250, etc.
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([\d,]+(?:\.\d{1,2})?)\s*(?:rupees|रुपये)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""paid\s+you\s+(?:₹|Rs\.?|INR)?\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""credited\s+(?:with|by)?\s*(?:₹|Rs\.?|INR)?\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    // Patterns for customer names
    private val CUSTOMER_FROM_PATTERNS = listOf(
        Pattern.compile("""from\s+([A-Za-z0-9\s.]{2,35})(?:\s+on|\s+via|\s+to|\s+ref|\s+for|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""by\s+([A-Za-z0-9\s.]{2,35})(?:\s+on|\s+via|\s+to|\s+ref|\s+for|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^([A-Za-z0-9\s.]{2,30})\s+(?:paid|sent|transferred)""", Pattern.CASE_INSENSITIVE)
    )

    // Positive indicators that this is a credit/received payment (and not a debit/sent payment)
    private val CREDIT_KEYWORDS = listOf(
        "received", "credited", "paid you", "deposited", "sent you", "has paid",
        "prapt", "प्राप्त", "payment of", "successful payment", "credit alert"
    )

    // Negative indicators: avoid debit notifications ("You paid", "Sent to", "Debited")
    private val DEBIT_KEYWORDS = listOf(
        "you paid", "debited from", "sent to", "transferred to", "recharge successful", "bill paid",
        "paid at", "withdrawn"
    )

    fun parse(sbn: StatusBarNotification): ParsedUpiPayment? {
        val packageName = sbn.packageName ?: return null
        val extras = sbn.notification?.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val combinedText = "$title | $text | $bigText | $subText".trim()
        val lowerText = combinedText.lowercase()

        // Check if package is known UPI app or text contains UPI/Bank keywords
        val isKnownApp = packageName.contains("google.android.apps.nbu.paisa") ||
                packageName.contains("phonepe") ||
                packageName.contains("paytm") ||
                packageName.contains("upiapp") ||
                packageName.contains("dreamplug")

        val hasCreditKeyword = CREDIT_KEYWORDS.any { lowerText.contains(it) }
        val hasDebitKeyword = DEBIT_KEYWORDS.any { lowerText.contains(it) }

        if (hasDebitKeyword && !hasCreditKeyword) {
            Log.d(TAG, "Ignoring debit notification: $title")
            return null
        }

        if (!isKnownApp && !hasCreditKeyword) {
            return null
        }

        val amount = extractAmount(combinedText) ?: return null
        val customer = extractCustomer(title, text, bigText)
        val app = UpiApp.fromPackageName(packageName)
        val refId = "UPI" + System.currentTimeMillis().toString().takeLast(10)

        return ParsedUpiPayment(
            amount = amount,
            customerName = customer,
            upiApp = app,
            referenceId = refId,
            rawText = combinedText
        )
    }

    fun parseRawText(rawText: String, forcedApp: UpiApp = UpiApp.GOOGLE_PAY): ParsedUpiPayment? {
        val amount = extractAmount(rawText) ?: return null
        val customer = extractCustomer(rawText, "", "")
        val refId = "UPI" + System.currentTimeMillis().toString().takeLast(10)

        return ParsedUpiPayment(
            amount = amount,
            customerName = customer,
            upiApp = forcedApp,
            referenceId = refId,
            rawText = rawText
        )
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = match?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractCustomer(title: String, text: String, bigText: String): String {
        val sources = listOf(text, bigText, title)
        for (source in sources) {
            for (pattern in CUSTOMER_FROM_PATTERNS) {
                val matcher = pattern.matcher(source)
                if (matcher.find()) {
                    val candidate = matcher.group(1)?.trim() ?: ""
                    val cleaned = candidate
                        .replace(Regex("""(?i)via|through|on|using|upi|bank|account|ref"""), "")
                        .trim()
                    if (cleaned.length in 3..28 && !cleaned.any { it.isDigit() && cleaned.length < 5 }) {
                        return cleaned.split(" ")
                            .filter { it.isNotBlank() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    }
                }
            }
        }

        // If title itself is a person's name (e.g. in GPay or PhonePe title is customer name)
        val cleanTitle = title.trim()
        if (cleanTitle.isNotEmpty() && !cleanTitle.contains("Payment", true) &&
            !cleanTitle.contains("Money", true) && !cleanTitle.contains("Google", true) &&
            !cleanTitle.contains("PhonePe", true) && !cleanTitle.contains("Paytm", true) &&
            cleanTitle.length in 3..25
        ) {
            return cleanTitle
        }

        return "UPI Customer"
    }

    fun toTransaction(parsed: ParsedUpiPayment, isSimulated: Boolean = false): PaymentTransaction {
        return PaymentTransaction(
            amount = parsed.amount,
            customerName = parsed.customerName,
            upiApp = parsed.upiApp.displayName,
            status = PaymentStatus.SUCCESS,
            timestamp = System.currentTimeMillis(),
            transactionRef = parsed.referenceId,
            rawNotificationText = parsed.rawText,
            isSimulated = isSimulated
        )
    }
}
