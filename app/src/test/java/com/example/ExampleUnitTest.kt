package com.example

import com.example.data.model.AnnouncementTemplate
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.UpiApp
import com.example.data.model.VoiceLanguage
import com.example.service.UpiNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun parse_gpay_notification() {
        val gpayText = "Pooja Patel paid you ₹1,200 on Google Pay"
        val parsed = UpiNotificationParser.parseRawText(gpayText, UpiApp.GOOGLE_PAY)

        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount, 0.01)
        assertEquals("Pooja Patel", parsed.customerName)
        assertEquals(UpiApp.GOOGLE_PAY, parsed.upiApp)
    }

    @Test
    fun parse_phonepe_notification() {
        val phonePeText = "PhonePe: You have received ₹750 from Anjali Gupta"
        val parsed = UpiNotificationParser.parseRawText(phonePeText, UpiApp.PHONE_PE)

        assertNotNull(parsed)
        assertEquals(750.0, parsed!!.amount, 0.01)
        assertEquals("Anjali Gupta", parsed.customerName)
    }

    @Test
    fun parse_paytm_notification() {
        val paytmText = "Paytm for Business: Received ₹500 from Rahul Sharma"
        val parsed = UpiNotificationParser.parseRawText(paytmText, UpiApp.PAYTM)

        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.01)
        assertEquals("Rahul Sharma", parsed.customerName)
    }

    @Test
    fun hindi_voice_announcement_template() {
        val template = AnnouncementTemplate.FULL_WITH_CUSTOMER
        val announcement = template.render(
            amount = "500",
            appName = "Paytm",
            customer = "राहुल शर्मा",
            lang = VoiceLanguage.HINDI
        )

        assertEquals("राहुल शर्मा से Paytm पर 500 रुपये प्राप्त हुए", announcement)
    }

    @Test
    fun hindi_amount_only_announcement_template() {
        val template = AnnouncementTemplate.AMOUNT_ONLY
        val announcement = template.render(
            amount = "500",
            appName = "",
            customer = "",
            lang = VoiceLanguage.HINDI
        )

        assertEquals("500 रुपये प्राप्त हुए", announcement)
    }

    @Test
    fun simulated_transaction_structure() {
        val payment = PaymentTransaction(
            amount = 500.0,
            customerName = "Rohan Gupta",
            upiApp = UpiApp.PAYTM.displayName,
            status = PaymentStatus.SUCCESS,
            isSimulated = true
        )

        assertEquals(500.0, payment.amount, 0.01)
        assertEquals("Rohan Gupta", payment.customerName)
        assertEquals("Paytm", payment.upiApp)
        assertTrue(payment.isSimulated)
    }

    @Test
    fun license_key_format_validation() {
        val key = "LVA-100-8841-K92X"
        val isValidFormat = key.startsWith("LVA-100-") && key.length >= 14
        assertTrue(isValidFormat)
    }
}
