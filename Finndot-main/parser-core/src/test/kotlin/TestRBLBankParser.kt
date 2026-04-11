package com.finndot.parser.core.test

import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.bank.RBLBankParser
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TestRBLBankParser {

    private val parser = RBLBankParser()
    private val sender = "AX-RBLBNK-S"

    @Test
    fun `parses RBL credit card spend with available limit`() {
        val msg = "INR100.00 spent at IDC KITCHEN PRIVATE on RBL Bank credit card (0859) on 02-11-2025.AVL limit- INR110,590.04. Not you? Call 02262327777"
        val parsed = parser.parse(msg, sender, System.currentTimeMillis())

        assert(parsed != null)
        assert(parsed?.type == TransactionType.EXPENSE)
        assert(parsed?.amount == BigDecimal("100.00"))
        assert(parsed?.merchant == "IDC KITCHEN PRIVATE")
        assert(parsed?.accountLast4 == "0859")
        assert(parsed?.creditLimit == BigDecimal("110590.04"))
        assert(parsed?.isFromCard == true)
        assert(parsed?.bankName == "RBL Bank")
    }
}


