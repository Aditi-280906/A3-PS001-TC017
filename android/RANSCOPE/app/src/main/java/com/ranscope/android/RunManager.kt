package com.ranscope.android

import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RunManager(
    private val baseDirectory: File
) {

    private val telemetryDirectory =
        File(
            baseDirectory,
            "telemetry"
        )

    private val manifestFile =
        File(
            telemetryDirectory,
            "run_manifest.jsonl"
        )

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            Locale.US
        )

    fun createRun(
        scenario: String
    ): RunInfo {

        val timestamp =
            System.currentTimeMillis()

        val runId =
            "RUN_$timestamp"

        val runDirectory =
            File(
                telemetryDirectory,
                runId
            )

        runDirectory.mkdirs()

        val telemetryFile =
            File(
                runDirectory,
                "events.jsonl"
            )

        val label =
            when {

                scenario.startsWith("B") ->
                    "benign"

                scenario.startsWith("R") ->
                    "ransomware_like"

                else ->
                    "unknown"
            }

        return RunInfo(
            runId = runId,
            scenario = scenario,
            label = label,
            startTimestamp = timestamp,
            telemetryFile = telemetryFile
        )
    }

    fun finishRun(
        runInfo: RunInfo,
        fileCount: Int,
        directoryCount: Int,
        status: String = "completed"
    ) {

        telemetryDirectory.mkdirs()

        val endTimestamp =
            System.currentTimeMillis()

        val record =
            JSONObject()

        record.put(
            "run_id",
            runInfo.runId
        )

        record.put(
            "scenario",
            runInfo.scenario
        )

        record.put(
            "label",
            runInfo.label
        )

        record.put(
            "start_timestamp_ms",
            runInfo.startTimestamp
        )

        record.put(
            "start_timestamp",
            dateFormat.format(
                Date(
                    runInfo.startTimestamp
                )
            )
        )

        record.put(
            "end_timestamp_ms",
            endTimestamp
        )

        record.put(
            "end_timestamp",
            dateFormat.format(
                Date(
                    endTimestamp
                )
            )
        )

        record.put(
            "duration_ms",
            endTimestamp -
                    runInfo.startTimestamp
        )

        record.put(
            "file_count",
            fileCount
        )

        record.put(
            "directory_count",
            directoryCount
        )

        record.put(
            "status",
            status
        )

        File(
            telemetryDirectory,
            "run_manifest.jsonl"
        ).appendText(
            record.toString() + "\n"
        )
    }

    fun clearDataset() {

        if (
            telemetryDirectory.exists()
        ) {

            telemetryDirectory
                .listFiles()
                ?.forEach { file ->

                    file.deleteRecursively()
                }
        }

        telemetryDirectory.mkdirs()
    }

    fun resetSandbox(
        sandboxDirectory: File
    ) {

        if (
            !sandboxDirectory.exists()
        ) {

            sandboxDirectory.mkdirs()

            return
        }

        sandboxDirectory
            .listFiles()
            ?.forEach { file ->

                file.deleteRecursively()
            }
    }
}

data class RunInfo(
    val runId: String,
    val scenario: String,
    val label: String,
    val startTimestamp: Long,
    val telemetryFile: File
)