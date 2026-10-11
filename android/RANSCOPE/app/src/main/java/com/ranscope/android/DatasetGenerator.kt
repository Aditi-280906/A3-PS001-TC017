package com.ranscope.android

import android.util.Log
import com.ranscope.android.simulator.AndroidBehaviorWorkload
import com.ranscope.android.simulator.B1BackupSimulator
import com.ranscope.android.simulator.B2ArchiveSimulator
import com.ranscope.android.simulator.DummyTreeBuilder
import com.ranscope.android.simulator.R1InplaceRewriteSimulator
import com.ranscope.android.simulator.R2CopyDeleteSimulator
import com.ranscope.android.simulator.R3StagedSlowSimulator
import com.ranscope.android.simulator.SandboxGuard
import com.ranscope.android.simulator.SimulatorConfig
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.random.Random


class DatasetGenerator(
    private val sandboxDirectory: File,
    private val runManager: RunManager,
    private val androidBehaviorWorkload: AndroidBehaviorWorkload? = null
) {

    @Volatile
    private var stopped = false


    // ========================================================
    // STOP
    // ========================================================

    fun stop() {

        stopped = true

        Log.d(
            "RANSCOPE_DATASET",
            "Dataset generation stop requested"
        )
    }


    // ========================================================
    // GENERATE NORMAL DATASET
    // ========================================================

    fun generateTestDataset(
        scenarioRuns: Map<String, Int>,
        clearExisting: Boolean = false,
        startRunNumber: Int = 1,
        onProgress: (String) -> Unit,
        onFinished: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        Thread {

            try {

                stopped = false

                Log.d(
                    "RANSCOPE_DATASET",
                    "Starting enriched Android telemetry dataset"
                )

                Log.d(
                    "RANSCOPE_DATASET",
                    "scenarioRuns=$scenarioRuns"
                )

                Log.d(
                    "RANSCOPE_DATASET",
                    "clearExisting=$clearExisting"
                )


                // ------------------------------------------------
                // PRESERVE OLD DATA BY DEFAULT
                // ------------------------------------------------

                if (clearExisting) {

                    runManager.clearDataset()

                } else {

                    Log.d(
                        "RANSCOPE_DATASET",
                        "Existing telemetry preserved"
                    )
                }


                // ------------------------------------------------
                // FIVE SCENARIOS
                // ------------------------------------------------

                val scenarios =
                    listOf(
                        "B1_backup_copy",
                        "B2_archive_compress",
                        "R1_inplace_rewrite",
                        "R2_copy_encrypt_delete",
                        "R3_staged_slow"
                    )


                // ------------------------------------------------
                // TOTAL NEW RUNS
                // ------------------------------------------------

                val totalRuns =
                    scenarioRuns.values.sum()

                var completedRuns =
                    0


                // ------------------------------------------------
                // SCENARIO LOOP
                // ------------------------------------------------

                for (scenario in scenarios) {

                    val numberOfRuns =
                        scenarioRuns[scenario] ?: 0

                    for (
                    localRunNumber
                    in 1..numberOfRuns
                    ) {

                        if (stopped) {

                            Log.d(
                                "RANSCOPE_DATASET",
                                "Generation stopped"
                            )

                            onFinished()

                            return@Thread
                        }


                        completedRuns++


                        val globalRunNumber =
                            startRunNumber +
                                    completedRuns -
                                    1


                        runScenario(

                            scenario =
                                scenario,

                            runNumber =
                                globalRunNumber,

                            totalRuns =
                                totalRuns,

                            completedRuns =
                                completedRuns,

                            onProgress =
                                onProgress
                        )


                        Thread.sleep(1000)
                    }
                }


                Log.d(
                    "RANSCOPE_DATASET",
                    "Dataset generation completed: " +
                            "$completedRuns runs"
                )


                onFinished()


            } catch (e: Exception) {

                Log.e(
                    "RANSCOPE_DATASET",
                    "Dataset generation failed",
                    e
                )

                onError(e)
            }

        }.start()
    }


    // ========================================================
    // GENERATE TRAINING DATASET
    //
    // IMPORTANT:
    // This is SEPARATE from generateTestDataset().
    //
    // It NEVER calls clearDataset().
    // It creates new RUN_* telemetry while preserving
    // everything already generated.
    //
    // Initial target:
    //
    // B1 = 16 runs
    // B2 = 114 runs
    // R1 = 10 runs
    // R2 = 10 runs
    // R3 = 15 runs
    //
    // These are only the FIRST generation estimates.
    // We will inspect actual event counts afterwards.
    // ========================================================

    fun generateTrainingDataset(
        b1Runs: Int = 16,
        b2Runs: Int = 114,
        r1Runs: Int = 10,
        r2Runs: Int = 10,
        r3Runs: Int = 15,
        startRunNumber: Int = 1,
        onProgress: (String) -> Unit,
        onFinished: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        Thread {

            try {

                stopped = false


                // ------------------------------------------------
                // TRAINING RUN CONFIGURATION
                // ------------------------------------------------

                val trainingScenarioRuns =
                    linkedMapOf(

                        "B1_backup_copy"
                                to b1Runs,

                        "B2_archive_compress"
                                to b2Runs,

                        "R1_inplace_rewrite"
                                to r1Runs,

                        "R2_copy_encrypt_delete"
                                to r2Runs,

                        "R3_staged_slow"
                                to r3Runs
                    )


                val totalRuns =
                    trainingScenarioRuns.values.sum()


                var completedRuns =
                    0


                Log.d(
                    "RANSCOPE_TRAINING",
                    "========================================"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "STARTING TRAINING DATASET"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "========================================"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "B1 runs = $b1Runs"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "B2 runs = $b2Runs"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "R1 runs = $r1Runs"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "R2 runs = $r2Runs"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "R3 runs = $r3Runs"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "TOTAL RUNS = $totalRuns"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "Existing telemetry WILL BE PRESERVED"
                )


                // ------------------------------------------------
                // SCENARIO ORDER
                // ------------------------------------------------

                val scenarios =
                    listOf(
                        "B1_backup_copy",
                        "B2_archive_compress",
                        "R1_inplace_rewrite",
                        "R2_copy_encrypt_delete",
                        "R3_staged_slow"
                    )


                // ------------------------------------------------
                // GENERATE EACH SCENARIO
                // ------------------------------------------------

                for (scenario in scenarios) {

                    val numberOfRuns =
                        trainingScenarioRuns[scenario]
                            ?: 0

                    for (
                    localRunNumber
                    in 1..numberOfRuns
                    ) {

                        if (stopped) {

                            Log.d(
                                "RANSCOPE_TRAINING",
                                "Training generation stopped"
                            )

                            onFinished()

                            return@Thread
                        }


                        completedRuns++


                        val globalRunNumber =
                            startRunNumber +
                                    completedRuns -
                                    1


                        Log.d(
                            "RANSCOPE_TRAINING",
                            "Starting training run " +
                                    "$completedRuns/$totalRuns | " +
                                    "scenario=$scenario | " +
                                    "runNumber=$globalRunNumber"
                        )


                        // ------------------------------------------------
                        // REUSE THE EXISTING, VALIDATED SCENARIO ENGINE
                        // ------------------------------------------------

                        runScenario(

                            scenario =
                                scenario,

                            runNumber =
                                globalRunNumber,

                            totalRuns =
                                totalRuns,

                            completedRuns =
                                completedRuns,

                            onProgress = { progress ->

                                onProgress(
                                    "TRAINING DATASET\n\n" +
                                            progress
                                )
                            }
                        )


                        // ------------------------------------------------
                        // GAP BETWEEN TRAINING RUNS
                        // ------------------------------------------------

                        Thread.sleep(1000)
                    }
                }


                Log.d(
                    "RANSCOPE_TRAINING",
                    "========================================"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "TRAINING DATASET GENERATION COMPLETE"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "Completed runs = $completedRuns"
                )

                Log.d(
                    "RANSCOPE_TRAINING",
                    "========================================"
                )


                onFinished()


            } catch (e: Exception) {

                Log.e(
                    "RANSCOPE_TRAINING",
                    "Training dataset generation failed",
                    e
                )

                onError(e)
            }

        }.start()
    }


    // ========================================================
    // DEDICATED EAP DATASET
    // ========================================================

    fun generateEapDataset(
        benignRuns: Int,
        maliciousRuns: Int,
        startRunNumber: Int = 1,
        onProgress: (String) -> Unit,
        onFinished: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        Thread {

            try {

                stopped = false

                val totalRuns =
                    benignRuns + maliciousRuns

                var completedRuns = 0


                Log.d(
                    "RANSCOPE_EAP",
                    "Starting dedicated EAP dataset"
                )

                Log.d(
                    "RANSCOPE_EAP",
                    "benignRuns=$benignRuns"
                )

                Log.d(
                    "RANSCOPE_EAP",
                    "maliciousRuns=$maliciousRuns"
                )


                // ====================================================
                // BENIGN EAP RUNS
                // ====================================================

                for (i in 1..benignRuns) {

                    if (stopped) {

                        onFinished()

                        return@Thread
                    }


                    completedRuns++


                    runDedicatedEapScenario(

                        scenario =
                            "EAP_BENIGN",

                        runNumber =
                            startRunNumber +
                                    completedRuns -
                                    1,

                        totalRuns =
                            totalRuns,

                        completedRuns =
                            completedRuns,

                        maliciousLike =
                            false,

                        onProgress =
                            onProgress
                    )


                    Thread.sleep(1000)
                }


                // ====================================================
                // MALICIOUS-LIKE EAP RUNS
                // ====================================================

                for (i in 1..maliciousRuns) {

                    if (stopped) {

                        onFinished()

                        return@Thread
                    }


                    completedRuns++


                    runDedicatedEapScenario(

                        scenario =
                            "EAP_MALICIOUS",

                        runNumber =
                            startRunNumber +
                                    completedRuns -
                                    1,

                        totalRuns =
                            totalRuns,

                        completedRuns =
                            completedRuns,

                        maliciousLike =
                            true,

                        onProgress =
                            onProgress
                    )


                    Thread.sleep(1000)
                }


                Log.d(
                    "RANSCOPE_EAP",
                    "Dedicated EAP dataset completed: " +
                            "$completedRuns runs"
                )


                onFinished()


            } catch (e: Exception) {

                Log.e(
                    "RANSCOPE_EAP",
                    "Dedicated EAP dataset failed",
                    e
                )

                onError(e)
            }

        }.start()
    }


    // ========================================================
    // SINGLE DEDICATED EAP RUN
    // ========================================================

    private fun runDedicatedEapScenario(
        scenario: String,
        runNumber: Int,
        totalRuns: Int,
        completedRuns: Int,
        maliciousLike: Boolean,
        onProgress: (String) -> Unit
    ) {

        Log.d(
            "RANSCOPE_EAP",
            "Preparing EAP run " +
                    "$completedRuns/$totalRuns " +
                    "run=$runNumber " +
                    "scenario=$scenario"
        )


        // ====================================================
        // 1. CLEAN ONLY TEST SANDBOX
        // ====================================================

        runManager.resetSandbox(
            sandboxDirectory
        )


        // ====================================================
        // 2. CREATE RUN
        // ====================================================

        val run =
            runManager.createRun(
                scenario
            )


        // ====================================================
        // 3. RANDOMIZE FILE TREE
        // ====================================================

        val random =
            Random(
                run.startTimestamp
            )

        val fileCount =
            random.nextInt(
                from = 70,
                until = 221
            )

        val directoryCount =
            random.nextInt(
                from = 5,
                until = 16
            )

        val config =
            SimulatorConfig(

                fileCount =
                    fileCount,

                directoryCount =
                    directoryCount,

                seed =
                    run.startTimestamp
            )


        val guard =
            SandboxGuard(
                sandboxDirectory
            )


        val builder =
            DummyTreeBuilder(
                guard,
                config
            )


        val generatedFiles =
            builder.build()


        Log.d(
            "RANSCOPE_EAP",
            "Dummy tree created: " +
                    "files=$fileCount " +
                    "directories=$directoryCount"
        )


        // ====================================================
        // 4. START FILE MONITOR
        // ====================================================

        val monitor =
            FileMonitor(
                sandboxDirectory
            )


        monitor.setRunContext(

            run.runId,

            scenario,

            run.telemetryFile
        )


        monitor.start()


        Log.d(
            "RANSCOPE_EAP",
            "Monitoring started: " +
                    run.runId
        )


        // ====================================================
        // 5. EAP OUTPUT FILE
        // ====================================================

        val eapFile =
            File(
                run.telemetryFile.parentFile,
                "eap_events.jsonl"
            )


        var capturedEapEvent:
                AndroidBehaviorWorkload.EapEvent? =
            null


        try {

            // =================================================
            // 6. UPDATE UI
            // =================================================

            onProgress(

                "EAP Run $completedRuns/$totalRuns\n" +

                        "Dataset run: #$runNumber\n" +

                        "$scenario\n" +

                        "Files: $fileCount\n" +

                        "Directories: $directoryCount\n" +

                        "Run: ${run.runId}"
            )


            // =================================================
            // 7. RUN REAL EAP WORKLOAD
            // =================================================

            val workload =
                androidBehaviorWorkload
                    ?: throw IllegalStateException(
                        "AndroidBehaviorWorkload is required " +
                                "for dedicated EAP collection"
                    )


            val latch =
                CountDownLatch(1)


            if (maliciousLike) {

                workload.runMaliciousLikeBehavior(

                    onFinished = {

                        latch.countDown()
                    },

                    onEapEvent = { event ->

                        capturedEapEvent =
                            event
                    }
                )

            } else {

                workload.runBenignBehavior(

                    onFinished = {

                        latch.countDown()
                    },

                    onEapEvent = { event ->

                        capturedEapEvent =
                            event
                    }
                )
            }


            // Wait for the Android workload to finish.
            latch.await()


            // =================================================
            // 8. REQUIRE EAP EVENT
            // =================================================

            val eapEvent =
                capturedEapEvent
                    ?: throw IllegalStateException(
                        "EAP Cipher.doFinal() was not captured " +
                                "for run ${run.runId}"
                    )


            // =================================================
            // 9. WRITE RUN-BOUND EAP EVENT
            // =================================================

            val eapJson =
                JSONObject()

                    .put(
                        "run_id",
                        run.runId
                    )

                    .put(
                        "scenario",
                        scenario
                    )

                    .put(
                        "label",
                        if (maliciousLike)
                            "malicious"
                        else
                            "benign"
                    )

                    .put(
                        "timestamp_ms",
                        eapEvent.timestampMs
                    )

                    .put(
                        "event",
                        eapEvent.event
                    )

                    .put(
                        "layer",
                        "EAP"
                    )

                    .put(
                        "package",
                        eapEvent.packageName
                    )

                    .put(
                        "algorithm",
                        eapEvent.algorithm
                    )

                    .put(
                        "input_length",
                        eapEvent.inputLength
                    )

                    .put(
                        "output_length",
                        eapEvent.outputLength
                    )


            eapFile.appendText(
                eapJson.toString() +
                        "\n"
            )


            Log.i(
                "RANSCOPE_EAP",
                "EAP EVENT SAVED | " +
                        "run=${run.runId} | " +
                        "scenario=$scenario | " +
                        "input=${eapEvent.inputLength} | " +
                        "output=${eapEvent.outputLength}"
            )


            // =================================================
            // 10. CONTROLLED FILESYSTEM WORKLOAD
            // =================================================

            if (maliciousLike) {

                val simulator =
                    R1InplaceRewriteSimulator(
                        guard
                    )


                simulator.run(
                    generatedFiles
                )

            } else {

                val simulator =
                    B1BackupSimulator(
                        guard
                    )


                simulator.run(
                    generatedFiles
                )
            }


            // =================================================
            // 11. FLUSH
            // =================================================

            Thread.sleep(
                500
            )


        } finally {

            // =================================================
            // 12. STOP MONITOR
            // =================================================

            monitor.stop()


            Log.d(
                "RANSCOPE_EAP",
                "Monitoring stopped: " +
                        run.runId
            )
        }


        // ====================================================
        // 13. SAVE RUN METADATA
        // ====================================================

        runManager.finishRun(

            runInfo =
                run,

            fileCount =
                fileCount,

            directoryCount =
                directoryCount
        )


        Log.d(
            "RANSCOPE_EAP",
            "EAP run completed: " +
                    run.runId
        )
    }


    // ========================================================
    // EXISTING SINGLE RUN
    // ========================================================

    private fun runScenario(
        scenario: String,
        runNumber: Int,
        totalRuns: Int,
        completedRuns: Int,
        onProgress: (String) -> Unit
    ) {

        Log.d(
            "RANSCOPE_DATASET",
            "Preparing run " +
                    "$completedRuns/$totalRuns " +
                    "datasetRun=$runNumber " +
                    "scenario=$scenario"
        )


        // ====================================================
        // 1. CLEAN ONLY THE TEST SANDBOX
        // ====================================================

        runManager.resetSandbox(
            sandboxDirectory
        )


        // ====================================================
        // 2. CREATE RUN
        // ====================================================

        val run =
            runManager.createRun(
                scenario
            )


        // ====================================================
        // 3. RANDOMIZE FILE TREE
        // ====================================================

        val random =
            Random(
                run.startTimestamp
            )


        val fileCount =
            random.nextInt(
                from = 70,
                until = 221
            )


        val directoryCount =
            random.nextInt(
                from = 5,
                until = 16
            )


        val config =
            SimulatorConfig(

                fileCount =
                    fileCount,

                directoryCount =
                    directoryCount,

                seed =
                    run.startTimestamp
            )


        // ====================================================
        // 4. BUILD TREE BEFORE MONITORING
        // ====================================================

        val guard =
            SandboxGuard(
                sandboxDirectory
            )


        val builder =
            DummyTreeBuilder(
                guard,
                config
            )


        val generatedFiles =
            builder.build()


        Log.d(
            "RANSCOPE_DATASET",
            "Dummy tree created: " +
                    "files=$fileCount " +
                    "directories=$directoryCount"
        )


        // ====================================================
        // 5. START FILE MONITOR
        // ====================================================

        val monitor =
            FileMonitor(
                sandboxDirectory
            )


        monitor.setRunContext(

            run.runId,

            scenario,

            run.telemetryFile
        )


        monitor.start()


        Log.d(
            "RANSCOPE_DATASET",
            "Monitoring started: " +
                    run.runId
        )


        // ====================================================
        // 5B. CREATE RUN-BOUND ANDROID TELEMETRY FILE
        // ====================================================

        val androidEventsFile =
            File(
                run.telemetryFile.parentFile,
                "android_events.jsonl"
            )


        if (androidEventsFile.exists()) {
            androidEventsFile.delete()
        }


        // ====================================================
        // 6. UPDATE UI
        // ====================================================

        onProgress(

            "Run $completedRuns/$totalRuns\n" +

                    "Dataset run: #$runNumber\n" +

                    "$scenario\n" +

                    "Files: $fileCount\n" +

                    "Directories: $directoryCount\n" +

                    "Run: ${run.runId}"
        )


        // ====================================================
        // 7. RUN ANDROID API BEHAVIOUR
        // ====================================================

        try {

            if (androidBehaviorWorkload != null) {

                val latch =
                    CountDownLatch(1)

                val maliciousLike =
                    scenario.startsWith("R")


                if (maliciousLike) {

                    androidBehaviorWorkload.runMaliciousLikeBehavior(

                        onFinished = {

                            latch.countDown()
                        },

                        onEapEvent = { event ->

                            writeAndroidEapEvent(
                                file =
                                    androidEventsFile,

                                runId =
                                    run.runId,

                                scenario =
                                    scenario,

                                label =
                                    "malicious",

                                event =
                                    event
                            )
                        },

                        onAndroidEvent = { event ->

                            writeAndroidEvent(
                                file =
                                    androidEventsFile,

                                runId =
                                    run.runId,

                                scenario =
                                    scenario,

                                label =
                                    "malicious",

                                event =
                                    event
                            )
                        }
                    )

                } else {

                    androidBehaviorWorkload.runBenignBehavior(

                        onFinished = {

                            latch.countDown()
                        },

                        onEapEvent = { event ->

                            writeAndroidEapEvent(
                                file =
                                    androidEventsFile,

                                runId =
                                    run.runId,

                                scenario =
                                    scenario,

                                label =
                                    "benign",

                                event =
                                    event
                            )
                        },

                        onAndroidEvent = { event ->

                            writeAndroidEvent(
                                file =
                                    androidEventsFile,

                                runId =
                                    run.runId,

                                scenario =
                                    scenario,

                                label =
                                    "benign",

                                event =
                                    event
                            )
                        }
                    )
                }


                latch.await()
            }


            // =================================================
            // 8. RUN FILESYSTEM WORKLOAD
            // =================================================

            when (scenario) {

                // ------------------------------------------------
                // B1
                // ------------------------------------------------

                "B1_backup_copy" -> {

                    val simulator =
                        B1BackupSimulator(
                            guard
                        )


                    simulator.run(
                        generatedFiles
                    )
                }


                // ------------------------------------------------
                // B2
                // ------------------------------------------------

                "B2_archive_compress" -> {

                    val simulator =
                        B2ArchiveSimulator(
                            guard
                        )


                    simulator.run(
                        generatedFiles
                    )
                }


                // ------------------------------------------------
                // R1
                // ------------------------------------------------

                "R1_inplace_rewrite" -> {

                    val simulator =
                        R1InplaceRewriteSimulator(
                            guard
                        )


                    simulator.run(
                        generatedFiles
                    )
                }


                // ------------------------------------------------
                // R2
                // ------------------------------------------------

                "R2_copy_encrypt_delete" -> {

                    val simulator =
                        R2CopyDeleteSimulator(
                            guard
                        )


                    simulator.run(
                        generatedFiles
                    )
                }


                // ------------------------------------------------
                // R3
                // ------------------------------------------------

                "R3_staged_slow" -> {

                    val simulator =
                        R3StagedSlowSimulator(
                            guard
                        )


                    var finished =
                        false


                    var error:
                            Exception? =
                        null


                    simulator.runAsync(

                        generatedFiles,

                        onFinished = {

                            finished =
                                true
                        },

                        onError = {

                            error =
                                it

                            finished =
                                true
                        }
                    )


                    while (!finished) {

                        Thread.sleep(
                            100
                        )
                    }


                    if (error != null) {

                        throw error!!
                    }
                }
            }


            // =================================================
            // 9. FILESYSTEM FLUSH DELAY
            // =================================================

            Thread.sleep(
                500
            )


        } finally {

            // =================================================
            // 10. ALWAYS STOP MONITOR
            // =================================================

            monitor.stop()


            Log.d(
                "RANSCOPE_DATASET",
                "Monitoring stopped: " +
                        run.runId
            )
        }


        // ====================================================
        // 11. SAVE RUN METADATA
        // ====================================================

        runManager.finishRun(

            runInfo =
                run,

            fileCount =
                fileCount,

            directoryCount =
                directoryCount
        )


        Log.d(
            "RANSCOPE_DATASET",
            "Run metadata saved: " +
                    run.runId
        )


        // ====================================================
        // 12. GAP BETWEEN RUNS
        // ====================================================

        Thread.sleep(
            1000
        )
    }


    // ========================================================
    // WRITE RUN-BOUND ANDROID TELEMETRY
    // ========================================================

    private fun writeAndroidEvent(
        file: File,
        runId: String,
        scenario: String,
        label: String,
        event: AndroidBehaviorWorkload.AndroidEvent
    ) {

        val json =
            JSONObject()
                .put(
                    "run_id",
                    runId
                )
                .put(
                    "scenario",
                    scenario
                )
                .put(
                    "label",
                    label
                )
                .put(
                    "timestamp_ms",
                    event.timestampMs
                )
                .put(
                    "layer",
                    event.layer
                )
                .put(
                    "event",
                    event.event
                )
                .put(
                    "package",
                    event.packageName
                )


        event.permission?.let {

            json.put(
                "permission",
                it
            )
        }


        event.granted?.let {

            json.put(
                "granted",
                it
            )
        }


        event.cameraId?.let {

            json.put(
                "camera_id",
                it
            )
        }


        event.cameraIds?.let {

            json.put(
                "camera_ids",
                it.joinToString(",")
            )
        }


        synchronized(file) {

            file.appendText(
                json.toString() +
                        "\n"
            )
        }
    }


    private fun writeAndroidEapEvent(
        file: File,
        runId: String,
        scenario: String,
        label: String,
        event: AndroidBehaviorWorkload.EapEvent
    ) {

        val json =
            JSONObject()
                .put(
                    "run_id",
                    runId
                )
                .put(
                    "scenario",
                    scenario
                )
                .put(
                    "label",
                    label
                )
                .put(
                    "timestamp_ms",
                    event.timestampMs
                )
                .put(
                    "layer",
                    "EAP"
                )
                .put(
                    "event",
                    event.event
                )
                .put(
                    "package",
                    event.packageName
                )
                .put(
                    "algorithm",
                    event.algorithm
                )
                .put(
                    "input_length",
                    event.inputLength
                )
                .put(
                    "output_length",
                    event.outputLength
                )


        synchronized(file) {

            file.appendText(
                json.toString() +
                        "\n"
            )
        }
    }


    // ========================================================
    // BENIGN ANDROID BEHAVIOUR
    // ========================================================

    private fun runBenignAndroidBehavior() {

        val latch =
            CountDownLatch(1)


        androidBehaviorWorkload?.runBenignBehavior {

            latch.countDown()
        }


        latch.await()
    }


    // ========================================================
    // MALICIOUS-LIKE ANDROID BEHAVIOUR
    // ========================================================

    private fun runMaliciousLikeAndroidBehavior() {

        val latch =
            CountDownLatch(1)


        androidBehaviorWorkload?.runMaliciousLikeBehavior {

            latch.countDown()
        }


        latch.await()
    }
}