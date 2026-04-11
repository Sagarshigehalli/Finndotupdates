package com.anomapro.finndot.domain.service

import com.finndot.parser.core.TransactionType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class LlmSmsParserTest {

    private lateinit var llmService: LlmService
    private lateinit var parser: LlmSmsParser
    private lateinit var logMock: org.mockito.MockedStatic<android.util.Log>

    @Before
    fun setup() {
        logMock = org.mockito.Mockito.mockStatic(android.util.Log::class.java)
        llmService = mock(LlmService::class.java)
        parser = LlmSmsParser(llmService)
    }

    @org.junit.After
    fun tearDown() {
        logMock.close()
    }

    @Test
    fun `parse returns null when service not initialized`() = runBlocking {
        `when`(llmService.isInitialized()).thenReturn(false)
        
        val result = parser.parse("test", "sender", 123L)
        assertNull(result)
    }

    @Test
    fun `parse handles valid json response`() = runBlocking {
        `when`(llmService.isInitialized()).thenReturn(true)
        val jsonResponse = """
            {
                "amount": "1500.50",
                "merchant": "Amazon",
                "type": "EXPENSE",
                "account": "4321",
                "balance": "10000.00",
                "category": "Shopping"
            }
        """
        `when`(llmService.generateResponse(any())).thenReturn(Result.success(jsonResponse))

        val result = parser.parse("Spent 1500.50 at Amazon", "Amazon", 123L)
        
        assertNotNull(result)
        assertEquals(java.math.BigDecimal("1500.50"), result?.amount)
        assertEquals("Amazon", result?.merchant)
        assertEquals(TransactionType.EXPENSE, result?.type)
        assertEquals("4321", result?.accountLast4)
        assertEquals(java.math.BigDecimal("10000.00"), result?.balance)
        assertEquals("Shopping", result?.category)
    }

    @Test
    fun `parse handles json with markdown code blocks`() = runBlocking {
        `when`(llmService.isInitialized()).thenReturn(true)
        val jsonResponse = """
            ```json
            {
                "amount": "500",
                "merchant": "Uber",
                "type": "EXPENSE",
                "category": "Travel"
            }
            ```
        """
        `when`(llmService.generateResponse(any())).thenReturn(Result.success(jsonResponse))

        val result = parser.parse("Uber ride 500", "Uber", 123L)
        
        assertNotNull(result)
        assertEquals(java.math.BigDecimal("500"), result?.amount)
        assertEquals("Travel", result?.category)
    }

    @Test
    fun `parse returns null for invalid json`() = runBlocking {
        `when`(llmService.isInitialized()).thenReturn(true)
        `when`(llmService.generateResponse(any())).thenReturn(Result.success("Not JSON"))

        val result = parser.parse("test", "sender", 123L)
        assertNull(result)
    }
}
