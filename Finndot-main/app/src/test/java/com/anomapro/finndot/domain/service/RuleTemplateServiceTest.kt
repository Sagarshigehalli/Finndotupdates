package com.anomapro.finndot.domain.service

import com.anomapro.finndot.domain.model.rule.ConditionOperator
import com.anomapro.finndot.domain.model.rule.TransactionField
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleTemplateServiceTest {

    private val service = RuleTemplateService()

    @Test
    fun defaultTemplates_includeSmsRailTransferTemplates() {
        val templates = service.getDefaultRuleTemplates()
        val smsTransfer = templates.filter { it.name.startsWith("Template:") }
        assertTrue("expected bank SMS templates", smsTransfer.size >= 8)
        smsTransfer.forEach { rule ->
            val smsCondition = rule.conditions.find {
                it.field == TransactionField.SMS_TEXT &&
                    it.operator == ConditionOperator.REGEX_MATCHES
            }
            assertTrue(rule.name, smsCondition != null)
        }
    }

    @Test
    fun sbiTemplateRegex_matchesTypicalNeftSms() {
        val regex = smsRegexForTemplateName("Template: SBI — NEFT/IMPS/RTGS (SMS)")
        val body =
            "SBI: Your A/c XX5678 credited with Rs 25,000 on 12-Apr-26 " +
                "by NEFT from JOHN DOE UTR SBINN22012345678 IFSC HDFC0000123"
        assertTrue(body.matches(Regex(regex)))
    }

    @Test
    fun hdfcTemplateRegex_matchesTypicalImpsSms() {
        val regex = smsRegexForTemplateName("Template: HDFC — NEFT/IMPS/RTGS (SMS)")
        val body =
            "HDFC Bank ALERT: Rs.3,500 debited from A/c XX9012 on 12-Apr-26 " +
                "towards IMPS to A/c XX3456 Ref 123456789012 IFSC UTIB0000001"
        assertTrue(body.matches(Regex(regex)))
    }

    @Test
    fun genericTemplateRegex_requiresRailPlusAnchor() {
        val regex = smsRegexForTemplateName("Template: Any bank — NEFT/IMPS/RTGS + IFSC/UTR (SMS)")
        val ok =
            "ALERT: Rs.1,000 debited for NEFT to beneficiary ABC IFSC SBIN0001234 Ref 998877"
        val missingAnchor = "ALERT: Rs.1,000 debited for some reason NEFT only text here"
        assertTrue(ok.matches(Regex(regex)))
        assertTrue(!missingAnchor.matches(Regex(regex)))
    }

    @Test
    fun bankTemplateRegex_excludesAmazonEvenWithNeftWord() {
        val regex = smsRegexForTemplateName("Template: ICICI — NEFT/IMPS/RTGS (SMS)")
        val merchant =
            "ICICI Bank: Rs.500 debited for Amazon order NEFT keyword in noise IFSC XXXX0001 UTR 1"
        assertTrue(!merchant.matches(Regex(regex)))
    }

    private fun smsRegexForTemplateName(name: String): String {
        val rule = service.getDefaultRuleTemplates().first { it.name == name }
        return rule.conditions.first {
            it.field == TransactionField.SMS_TEXT &&
                it.operator == ConditionOperator.REGEX_MATCHES
        }.value
    }
}
