package com.braintreepayments.api.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostRequestExecutorUnitTest {

    private val responseParser = mockk<ResponseParser>(relaxed = true)

    private lateinit var subject: PostRequestExecutor

    val connection = mockk<HttpsURLConnection>(relaxed = true)
    private val url = mockk<URL>(relaxed = true)
    val jsonBody = "jsonBody"

    private fun initSubject(testScheduler: TestCoroutineScheduler) {
        mockkConstructor(OutputStreamWriter::class)
        every { url.openConnection() } returns connection
        every { connection.responseCode } returns 200

        subject = PostRequestExecutor(responseParser, StandardTestDispatcher(testScheduler))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `calling execute sets the correct properties on HttpURLConnection`() = runTest {
        initSubject(testScheduler)

        subject.execute(url, jsonBody)

        verify {
            with(connection) {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("Accept-Language", Locale.getDefault().language)
            }
        }
    }

    @Test
    fun `calling execute with a HttpURLConnection throws an IllegalArgumentException`() = runTest {
        initSubject(testScheduler)
        every { url.openConnection() } returns mockk<HttpURLConnection>()

        val exception = assertFailsWith<IllegalArgumentException> {
            subject.execute(url, jsonBody)
        }

        assertEquals("Only HTTPS is supported", exception.message)
    }

    @Test
    fun `calling execute writes, flushes, and closes the OutputStreamWriter`() = runTest {
        initSubject(testScheduler)
        val outputStream = mockk<OutputStream>(relaxed = true)
        every { connection.outputStream } returns outputStream

        subject.execute(url, jsonBody)

        verify {
            OutputStreamWriter(outputStream).write(jsonBody)
            OutputStreamWriter(outputStream).flush()
            OutputStreamWriter(outputStream).close()
        }
    }

    @Test
    fun `calling execute with a 200 response and non gzip encoding, returns the response`() = runTest {
        initSubject(testScheduler)
        val expectedResponse = "expectedResponse"
        val inputStream = mockk<InputStream>(relaxed = true)
        every { connection.inputStream } returns inputStream
        every { connection.contentEncoding } returns "notGzip"
        every { connection.responseCode } returns 200
        every { (responseParser.parse(inputStream, false)) } returns expectedResponse

        val result = subject.execute(url, jsonBody)

        assertEquals(result, expectedResponse)
    }

    @Test
    fun `calling execute with a 200 response and gzip encoding, returns the response`() = runTest {
        initSubject(testScheduler)
        val expectedResponse = "expectedResponse"
        val inputStream = mockk<InputStream>(relaxed = true)
        every { connection.inputStream } returns inputStream
        every { connection.contentEncoding } returns "gzip"
        every { connection.responseCode } returns 200
        every { (responseParser.parse(inputStream, true)) } returns expectedResponse

        val result = subject.execute(url, jsonBody)

        assertEquals(result, expectedResponse)
    }

    @Test
    fun `calling execute with a non 2xx response throws an exception`() = runTest {
        initSubject(testScheduler)
        val expectedErrorResponse = "expectedErrorResponse"
        val inputStream = mockk<InputStream>(relaxed = true)
        every { connection.responseCode } returns 300
        every { connection.errorStream } returns inputStream
        every { connection.contentEncoding } returns "gzip"
        every { (responseParser.parse(inputStream, true)) } returns expectedErrorResponse

        val exception = assertFailsWith<IOException> {
            subject.execute(url, jsonBody)
        }

        assertEquals("HTTP 300: $expectedErrorResponse", exception.message)
    }

    @Test
    fun `calling execute calls connection disconnect`() = runTest {
        initSubject(testScheduler)

        subject.execute(url, jsonBody)

        verify { connection.disconnect() }
    }
}