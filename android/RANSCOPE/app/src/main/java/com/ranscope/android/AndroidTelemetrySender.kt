package com.ranscope.android

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.concurrent.Executors

object AndroidTelemetrySender {

    private const val TAG = "RANSCOPE_NETWORK"

    // adb reverse tcp:8000 tcp:8000 maps this to the Mac backend.
    private const val ENDPOINT =
        "http://127.0.0.1:8000/api/telemetry/window"
    private val networkExecutor =
        Executors.newSingleThreadExecutor()

    fun sendWindow(
        runId: String,
        scenario: String,
        windowStartMs: Long,
        windowEndMs: Long,
        features: Map<String, Double>
    ) {
        networkExecutor.execute {
            sendWindowBlocking(
                runId,
                scenario,
                windowStartMs,
                windowEndMs,
                features
            )
        }
    }

    private fun sendWindowBlocking(
        runId: String,
        scenario: String,
        windowStartMs: Long,
        windowEndMs: Long,
        features: Map<String, Double>
    ) {
        val telemetry = JSONObject().apply {
            put("event", "android_window_complete")
            put("layer", "ANDROID_FILE_MONITOR")
            put("run_id", runId)
            put("scenario", scenario)
            put("window_start_ms", windowStartMs)
            put("window_end_ms", windowEndMs)
            put("window_duration_ms", windowEndMs - windowStartMs)
            put("feature_window", JSONObject(features))
        }

        val payload = JSONObject().apply {
            put("device_id", "ranscope-android")
            put("timestamp", Instant.now().toString())
            put("telemetry", telemetry)
        }

        var connection: HttpURLConnection? = null

        try {
            connection = (URL(ENDPOINT).openConnection()
                    as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                setRequestProperty("Accept", "application/json")
            }

            connection.outputStream.use { output ->
                output.write(
                    payload.toString().toByteArray(Charsets.UTF_8)
                )
            }

            val status = connection.responseCode

            if (status in 200..299) {
                Log.i(
                    TAG,
                    "Window sent successfully: $runId, " +
                            "$windowStartMs-$windowEndMs, HTTP $status"
                )
            } else {
                val errorBody = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                Log.e(
                    TAG,
                    "Window send failed: HTTP $status $errorBody"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not send Android window", e)
        } finally {
            connection?.disconnect()
        }
    }
}