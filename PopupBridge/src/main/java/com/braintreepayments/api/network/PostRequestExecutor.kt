package com.braintreepayments.api.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * Executes a POST request and returns the response.
 */
internal class PostRequestExecutor(
    private val socketFactory: SSLSocketFactory = TLSSocketFactory(TLSCertificate.createCertificateInputStream()),
    private val responseParser: ResponseParser = ResponseParser(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun execute(
        url: URL,
        jsonBody: String,
    ) = withContext(dispatcher) {
        var connection: HttpURLConnection? = null
        try {
            connection = (url.openConnection() as? HttpsURLConnection)?.apply {
                sslSocketFactory = socketFactory
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("Accept-Language", Locale.getDefault().language)
            } ?: throw IllegalArgumentException("Only HTTPS is supported")

            // Write JSON Body
            OutputStreamWriter(connection.outputStream).apply {
                write(jsonBody)
                flush()
                close()
            }

            // Read Response
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            // Parse Response
            val response = responseParser.parse(
                inputStream = inputStream,
                isGzipEncoded = connection.contentEncoding == "gzip"
            )

            if (responseCode in 200..299) {
                response
            } else {
                throw IOException("HTTP $responseCode: $response")
            }
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 10000
    }
}
