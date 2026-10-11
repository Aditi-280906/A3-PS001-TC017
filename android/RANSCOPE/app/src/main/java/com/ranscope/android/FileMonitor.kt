package com.ranscope.android

import android.os.FileObserver
import android.util.Log
import com.ranscope.android.detection.TemporalWindow
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileMonitor(
    private val directory: File,
    private val onWindowComplete:
    ((TemporalWindow, Map<String, Double>) -> Unit)? = null
) {

    // ========================================================
    // FILE OBSERVERS
    // ========================================================

    private val observers =
        mutableMapOf<String, FileObserver>()


    // ========================================================
    // RUN CONTEXT
    // ========================================================

    private var currentRunId =
        "NO_RUN"

    private var currentScenario =
        "SETUP"

    private var telemetryFile: File? =
        null

    private var runStartTime =
        System.currentTimeMillis()


    // ========================================================
    // TEMPORAL WINDOW
    // ========================================================

    private val windowLock =
        Any()

    private var currentWindow:
            TemporalWindow? = null

    private val windowDurationMs =
        5_000L


    // ========================================================
    // DATE FORMAT
    // ========================================================

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            Locale.US
        )


    // ========================================================
    // SET RUN CONTEXT
    // ========================================================

    fun setRunContext(
        runId: String,
        scenario: String,
        runTelemetryFile: File
    ) {

        currentRunId =
            runId

        currentScenario =
            scenario

        telemetryFile =
            runTelemetryFile

        runStartTime =
            System.currentTimeMillis()

        synchronized(windowLock) {

            currentWindow =
                TemporalWindow(
                    windowStartMs = 0L,
                    windowEndMs =
                        windowDurationMs
                )
        }

        Log.d(
            "RANSCOPE",
            "Run context: " +
                    "runId=$runId " +
                    "scenario=$scenario"
        )

        Log.d(
            "RANSCOPE",
            "Telemetry file: " +
                    "${runTelemetryFile.absolutePath}"
        )
    }


    // ========================================================
    // CREATE OBSERVER
    // ========================================================

    private fun createObserver(
        watchedDirectory: File
    ): FileObserver {

        return object : FileObserver(
            watchedDirectory,
            CREATE or
                    MODIFY or
                    CLOSE_WRITE or
                    DELETE or
                    MOVED_FROM or
                    MOVED_TO
        ) {

            override fun onEvent(
                event: Int,
                path: String?
            ) {

                if (path == null) {
                    return
                }

                val baseEvent =
                    event and ALL_EVENTS

                val eventName =
                    when (baseEvent) {

                        CREATE ->
                            "CREATE"

                        MODIFY ->
                            "MODIFY"

                        CLOSE_WRITE ->
                            "CLOSE_WRITE"

                        DELETE ->
                            "DELETE"

                        MOVED_FROM ->
                            "MOVED_FROM"

                        MOVED_TO ->
                            "MOVED_TO"

                        else ->
                            "OTHER"
                    }


                val changedFile =
                    File(
                        watchedDirectory,
                        path
                    )


                // ------------------------------------------------
                // Watch newly created directories
                // ------------------------------------------------

                if (
                    baseEvent == CREATE &&
                    changedFile.isDirectory
                ) {

                    addDirectory(
                        changedFile
                    )
                }


                // ------------------------------------------------
                // Watch directories moved into sandbox
                // ------------------------------------------------

                if (
                    baseEvent == MOVED_TO &&
                    changedFile.isDirectory
                ) {

                    addDirectory(
                        changedFile
                    )
                }


                // ------------------------------------------------
                // Raw telemetry
                // ------------------------------------------------

                writeTelemetry(
                    eventName,
                    changedFile
                )


                // ------------------------------------------------
                // Temporal telemetry
                // ------------------------------------------------

                addEventToTemporalWindow(
                    eventName,
                    changedFile
                )


                Log.d(
                    "RANSCOPE",
                    "event=$eventName " +
                            "file=${changedFile.absolutePath}"
                )
            }
        }
    }


    // ========================================================
    // ADD EVENT TO TEMPORAL WINDOW
    // ========================================================

    private fun addEventToTemporalWindow(
        eventName: String,
        file: File
    ) {

        val elapsedMs =
            System.currentTimeMillis() -
                    runStartTime


        synchronized(windowLock) {

            var window =
                currentWindow
                    ?: TemporalWindow(
                        windowStartMs = 0L,
                        windowEndMs =
                            windowDurationMs
                    )


            // ------------------------------------------------
            // Complete old windows
            // ------------------------------------------------

            while (
                elapsedMs >=
                window.windowEndMs
            ) {

                completeTemporalWindow(
                    window
                )

                val nextStart =
                    window.windowEndMs

                window =
                    TemporalWindow(
                        windowStartMs =
                            nextStart,
                        windowEndMs =
                            nextStart +
                                    windowDurationMs
                    )

                currentWindow =
                    window
            }


            // ------------------------------------------------
            // File information
            // ------------------------------------------------

            val depth =
                calculateDepth(
                    file
                )

            val fileSize =
                getFileSize(
                    file
                )


            // ------------------------------------------------
            // Entropy
            //
            // We attempt entropy measurement for actual
            // file-content events.
            //
            // Missing entropy is left as null.
            // We NEVER manufacture entropy.
            // ------------------------------------------------

            val writeEntropy =
                calculateTelemetryEntropy(
                    eventName,
                    file
                )


            // ------------------------------------------------
            // CLOSE_WRITE is represented as MODIFY inside
            // TemporalWindow.
            //
            // Raw telemetry still preserves CLOSE_WRITE.
            // ------------------------------------------------

            val temporalEvent =
                if (
                    eventName ==
                    "CLOSE_WRITE"
                ) {

                    "MODIFY"

                } else {

                    eventName
                }


            // ------------------------------------------------
            // Add to temporal window
            // ------------------------------------------------

            window.addFileEvent(

                event =
                    temporalEvent,

                path =
                    file.absolutePath,

                depth =
                    depth,

                fileSize =
                    fileSize,

                dirEntropy =
                    0.0,

                writeEntropy =
                    writeEntropy
            )


            if (
                writeEntropy != null
            ) {

                Log.d(
                    "RANSCOPE_EAP",
                    "Entropy accepted: " +
                            "event=$eventName " +
                            "file=${file.name} " +
                            "entropy=" +
                            String.format(
                                Locale.US,
                                "%.4f",
                                writeEntropy
                            )
                )
            }
        }
    }


    // ========================================================
    // ENTROPY CALCULATION
    // ========================================================

    private fun calculateTelemetryEntropy(
        eventName: String,
        file: File
    ): Double? {

        /*
         * We calculate entropy only for events that can
         * correspond to file-content activity.
         *
         * Directory events naturally produce null.
         */

        if (
            eventName != "CREATE" &&
            eventName != "MODIFY" &&
            eventName != "CLOSE_WRITE" &&
            eventName != "MOVED_TO"
        ) {

            return null
        }


        return try {

            if (
                file.exists() &&
                file.isFile &&
                file.length() > 0L
            ) {

                EntropyCalculator
                    .calculateFileEntropy(
                        file
                    )

            } else {

                null
            }

        } catch (e: Exception) {

            Log.e(
                "RANSCOPE_EAP",
                "Entropy calculation failed " +
                        "for ${file.absolutePath}",
                e
            )

            null
        }
    }


    // ========================================================
    // COMPLETE TEMPORAL WINDOW
    // ========================================================

    private fun completeTemporalWindow(
        window: TemporalWindow
    ) {

        val features =
            window.toFeatureMap()


        Log.d(
            "RANSCOPE_TEMPORAL",
            "Window completed: " +
                    "${window.windowStartMs}-" +
                    "${window.windowEndMs}"
        )

        Log.d(
            "RANSCOPE_TEMPORAL",
            "SFIM events=" +
                    window.sfimEventCount +
                    " writes=" +
                    window.sfimWriteCount +
                    " renames=" +
                    window.sfimRenameCount +
                    " deletes=" +
                    window.sfimUnlinkCount +
                    " creates=" +
                    window.sfimCreateCount
        )

        Log.d(
            "RANSCOPE_TEMPORAL",
            "EAP H=" +
                    window.eapEntropy() +
                    " dH=" +
                    window.eapDHEntropy() +
                    " d2H=" +
                    window.eapD2HEntropy() +
                    " score=" +
                    window.eapScore()
        )

        Log.d(
            "RANSCOPE_TEMPORAL",
            "Files changed=" +
                    window.uniqueFilesChanged()
        )


        onWindowComplete?.invoke(
            window,
            features
        )
    }


    // ========================================================
    // ADD DIRECTORY
    // ========================================================

    private fun addDirectory(
        directoryToWatch: File
    ) {

        if (
            !directoryToWatch.exists() ||
            !directoryToWatch.isDirectory
        ) {

            return
        }


        val key =
            directoryToWatch.absolutePath


        if (
            observers.containsKey(key)
        ) {

            return
        }


        val observer =
            createObserver(
                directoryToWatch
            )


        observers[key] =
            observer


        observer.startWatching()


        Log.d(
            "RANSCOPE",
            "Watching directory: $key"
        )


        // ------------------------------------------------
        // Recursively watch existing children
        // ------------------------------------------------

        directoryToWatch
            .listFiles()
            ?.filter {
                it.isDirectory
            }
            ?.forEach { child ->

                addDirectory(
                    child
                )
            }
    }


    // ========================================================
    // CALCULATE DEPTH
    // ========================================================

    private fun calculateDepth(
        file: File
    ): Int {

        return try {

            file.relativeTo(
                directory
            )
                .parentFile
                ?.toPath()
                ?.nameCount
                ?: 0

        } catch (e: Exception) {

            0
        }
    }


    // ========================================================
    // FILE EXTENSION
    // ========================================================

    private fun getExtension(
        file: File
    ): String {

        val name =
            file.name

        val lastDot =
            name.lastIndexOf('.')


        if (
            lastDot <= 0 ||
            lastDot >= name.length - 1
        ) {

            return ""
        }


        return name
            .substring(lastDot)
            .lowercase(
                Locale.US
            )
    }


    // ========================================================
    // FILE SIZE
    // ========================================================

    private fun getFileSize(
        file: File
    ): Long {

        return try {

            if (
                file.exists() &&
                file.isFile
            ) {

                file.length()

            } else {

                0L
            }

        } catch (e: Exception) {

            0L
        }
    }


    // ========================================================
    // RAW TELEMETRY
    // ========================================================

    private fun writeTelemetry(
        eventName: String,
        file: File
    ) {

        val outputFile =
            telemetryFile
                ?: return


        val timestamp =
            System.currentTimeMillis()


        val elapsed =
            timestamp -
                    runStartTime


        val timeString =
            synchronized(
                dateFormat
            ) {

                dateFormat.format(
                    Date(
                        timestamp
                    )
                )
            }


        val relativePath =
            try {

                file.relativeTo(
                    directory
                ).path

            } catch (e: Exception) {

                file.name
            }


        val extension =
            getExtension(
                file
            )


        val depth =
            calculateDepth(
                file
            )


        val fileSize =
            getFileSize(
                file
            )


        val entropy =
            calculateTelemetryEntropy(
                eventName,
                file
            )


        val record =
            JSONObject()


        record.put(
            "run_id",
            currentRunId
        )

        record.put(
            "scenario",
            currentScenario
        )

        record.put(
            "timestamp_ms",
            timestamp
        )

        record.put(
            "timestamp",
            timeString
        )

        record.put(
            "elapsed_ms",
            elapsed
        )

        record.put(
            "event",
            eventName
        )

        record.put(
            "path",
            relativePath
        )

        record.put(
            "extension",
            extension
        )

        record.put(
            "depth",
            depth
        )

        record.put(
            "file_size",
            fileSize
        )

        record.put(
            "entropy",
            entropy ?: JSONObject.NULL
        )


        try {

            outputFile
                .parentFile
                ?.mkdirs()


            FileWriter(
                outputFile,
                true
            ).use { writer ->

                writer.write(
                    record.toString()
                )

                writer.write("\n")
            }

        } catch (e: Exception) {

            Log.e(
                "RANSCOPE",
                "Failed to write telemetry",
                e
            )
        }
    }


    // ========================================================
    // START
    // ========================================================

    fun start() {

        if (
            !directory.exists()
        ) {

            directory.mkdirs()
        }


        addDirectory(
            directory
        )


        Log.d(
            "RANSCOPE",
            "Recursive file monitoring started"
        )
    }


    // ========================================================
    // STOP
    // ========================================================

    fun stop() {

        synchronized(
            windowLock
        ) {

            currentWindow?.let {

                completeTemporalWindow(
                    it
                )
            }

            currentWindow =
                null
        }


        observers.values.forEach {
                observer ->

            observer.stopWatching()
        }


        observers.clear()


        Log.d(
            "RANSCOPE",
            "All file monitoring stopped"
        )
    }
}