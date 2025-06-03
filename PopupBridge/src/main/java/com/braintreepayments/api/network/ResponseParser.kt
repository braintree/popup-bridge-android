package com.braintreepayments.api.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

/**
 * Parses the response from an HTTP request.
 */
internal class ResponseParser {

    fun parse(
        inputStream: InputStream,
        isGzipEncoded: Boolean,
    ): String {
        val input = if (isGzipEncoded) {
            GZIPInputStream(inputStream)
        } else {
            inputStream
        }

        try {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                output.write(buffer, 0, count)
            }
            return String(output.toByteArray(), StandardCharsets.UTF_8)
        } finally {
            try {
                input.close()
            } catch (_: IOException) {
            }
        }
    }
}
