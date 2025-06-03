package com.braintreepayments.api.network

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ResponseParserUnitTest {

    private lateinit var responseParser: ResponseParser

    @Before
    fun setUp() {
        responseParser = ResponseParser()
    }

    @Test
    fun `parse should return empty string when input stream is empty`() {
        val emptyInputStream = ByteArrayInputStream(ByteArray(0))
        val result = responseParser.parse(emptyInputStream, false)
        assertEquals("", result)
    }

    @Test
    fun `parse should return correct string when input stream is not gzipped`() {
        val expected = "expected string"
        val inputStream = ByteArrayInputStream(expected.toByteArray())
        val result = responseParser.parse(inputStream, false)
        assertEquals(expected, result)
    }

    @Test
    fun `parse should return correct string when input stream is gzipped`() {
        val expected = "expected string"
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
            gzipOutputStream.write(expected.toByteArray())
        }
        val inputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        val result = responseParser.parse(inputStream, true)
        assertEquals(expected, result)
    }
}
