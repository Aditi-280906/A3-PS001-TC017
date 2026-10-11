package com.ranscope.android

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.ranscope.android.ui.theme.RANSCOPETheme

import com.ranscope.android.simulator.SandboxGuard
import com.ranscope.android.simulator.SimulatorConfig
import com.ranscope.android.simulator.DummyTreeBuilder

import com.ranscope.android.simulator.B1BackupSimulator
import com.ranscope.android.simulator.B2ArchiveSimulator
import com.ranscope.android.simulator.R1InplaceRewriteSimulator
import com.ranscope.android.simulator.R2CopyDeleteSimulator
import com.ranscope.android.simulator.R3StagedSlowSimulator

import com.ranscope.android.simulator.AndroidBehaviorWorkload

import java.io.File


class MainActivity : ComponentActivity() {

    private var fileMonitor: FileMonitor? = null

    private var generatedFiles: List<File> =
        emptyList()

    private lateinit var testDirectory: File

    private lateinit var runManager: RunManager

    // ====================================================
    // SEPARATE UNSEEN TEST DATASET DIRECTORY
    // ====================================================

    private lateinit var unseenTestDirectory: File

    private lateinit var unseenTestRunManager: RunManager

    private var datasetGenerator: DatasetGenerator? =
        null

    private lateinit var androidBehaviorWorkload:
            AndroidBehaviorWorkload


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        // ====================================================
        // EXISTING TEST DIRECTORY
        // ====================================================

        testDirectory =
            File(
                getExternalFilesDir(null),
                "RANSCOPE_TEST"
            )


        // ====================================================
        // EXISTING RUN MANAGER
        // ====================================================

        runManager =
            RunManager(
                File(
                    getExternalFilesDir(null),
                    ""
                )
            )


        // ====================================================
        // SEPARATE UNSEEN TEST DIRECTORY
        // ====================================================

        unseenTestDirectory =
            File(
                getExternalFilesDir(null),
                "RANSCOPE_UNSEEN_TEST"
            )

        unseenTestRunManager =
            RunManager(
                unseenTestDirectory
            )

        unseenTestDirectory.mkdirs()


        // ====================================================
        // ANDROID BEHAVIOUR WORKLOAD
        // ====================================================

        androidBehaviorWorkload =
            AndroidBehaviorWorkload(this)


        // ====================================================
        // UI
        // ====================================================

        setContent {

            RANSCOPETheme {

                RANSCOPEScreen()
            }
        }
    }


    @Composable
    fun RANSCOPEScreen() {

        var status by remember {

            mutableStateOf(
                "Ready"
            )
        }


        // ====================================================
        // DATASET GENERATION STATE
        // ====================================================

        var datasetRunning by remember {

            mutableStateOf(
                false
            )
        }


        var datasetType by remember {

            mutableStateOf(
                "IDLE"
            )
        }


        var currentScenario by remember {

            mutableStateOf(
                "-"
            )
        }


        var currentRun by remember {

            mutableStateOf(
                0
            )
        }


        var totalRuns by remember {

            mutableStateOf(
                0
            )
        }


        var currentRunId by remember {

            mutableStateOf(
                "-"
            )
        }


        var currentStage by remember {

            mutableStateOf(
                "Ready"
            )
        }


        var completedRuns by remember {

            mutableStateOf(
                0
            )
        }


        // ====================================================
        // PARSE DATASET PROGRESS
        // ====================================================

        fun updateDatasetProgress(
            progress: String
        ) {

            status =
                progress


            // ------------------------------------------------
            // DATASET TYPE
            // ------------------------------------------------

            if (
                progress.contains(
                    "TRAINING DATASET",
                    ignoreCase = true
                )
            ) {

                datasetType =
                    "TRAINING DATASET"

            } else if (
                progress.contains(
                    "EAP",
                    ignoreCase = true
                )
            ) {

                datasetType =
                    "EAP DATASET"

            } else if (
                progress.contains(
                    "FINAL DATASET",
                    ignoreCase = true
                )
            ) {

                datasetType =
                    "FINAL DATASET"

            } else if (
                progress.contains(
                    "UNSEEN TEST",
                    ignoreCase = true
                )
            ) {

                datasetType =
                    "UNSEEN TEST DATASET"
            }


            // ------------------------------------------------
            // SCENARIO
            // ------------------------------------------------

            val scenarios =
                listOf(

                    "B1_backup_copy",

                    "B2_archive_compress",

                    "R1_inplace_rewrite",

                    "R2_copy_encrypt_delete",

                    "R3_staged_slow",

                    "EAP_BENIGN",

                    "EAP_MALICIOUS"
                )


            for (
            scenario in scenarios
            ) {

                if (
                    progress.contains(
                        scenario,
                        ignoreCase = true
                    )
                ) {

                    currentScenario =
                        scenario

                    break
                }
            }


            // ------------------------------------------------
            // RUN X / Y
            // ------------------------------------------------

            val runRegex =
                Regex(
                    """(?:Run|run)\s+(\d+)\s*/\s*(\d+)"""
                )


            val runMatch =
                runRegex.find(
                    progress
                )


            if (
                runMatch != null
            ) {

                currentRun =
                    runMatch
                        .groupValues[1]
                        .toIntOrNull()
                        ?: currentRun


                totalRuns =
                    runMatch
                        .groupValues[2]
                        .toIntOrNull()
                        ?: totalRuns


                completedRuns =
                    (
                            currentRun - 1
                            )
                        .coerceAtLeast(0)
            }


            // ------------------------------------------------
            // DATASET RUN NUMBER
            // ------------------------------------------------

            val datasetRunRegex =
                Regex(
                    """Dataset run:\s*#(\d+)"""
                )


            val datasetRunMatch =
                datasetRunRegex.find(
                    progress
                )


            if (
                datasetRunMatch != null
            ) {

                currentRun =
                    datasetRunMatch
                        .groupValues[1]
                        .toIntOrNull()
                        ?: currentRun
            }


            // ------------------------------------------------
            // RUN ID
            // ------------------------------------------------

            val runIdRegex =
                Regex(
                    """RUN_\d+"""
                )


            val runIdMatch =
                runIdRegex.find(
                    progress
                )


            if (
                runIdMatch != null
            ) {

                currentRunId =
                    runIdMatch.value
            }


            // ------------------------------------------------
            // STAGE
            // ------------------------------------------------

            currentStage =
                when {

                    progress.contains(
                        "Preparing run",
                        ignoreCase = true
                    ) ->
                        "Preparing run"


                    progress.contains(
                        "Files:",
                        ignoreCase = true
                    ) &&
                            progress.contains(
                                "Directories:",
                                ignoreCase = true
                            ) ->
                        "Preparing filesystem"


                    progress.contains(
                        "EAP Run",
                        ignoreCase = true
                    ) ->
                        "Running EAP behaviour"


                    progress.contains(
                        "Monitoring started",
                        ignoreCase = true
                    ) ->
                        "Monitoring"


                    progress.contains(
                        "Run ",
                        ignoreCase = true
                    ) ->
                        "Running workload"


                    else ->
                        currentStage
                }
        }


        // ====================================================
        // PROGRESS
        // ====================================================

        val progressFraction =

            if (
                totalRuns > 0
            ) {

                (
                        completedRuns.toFloat() /
                                totalRuns.toFloat()
                        )
                    .coerceIn(
                        0f,
                        1f
                    )

            } else {

                0f
            }


        // ====================================================
        // MAIN UI
        // ====================================================

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Top

        ) {


            // =================================================
            // TITLE
            // =================================================

            Text(
                text = "RANSCOPE"
            )


            Text(

                text =
                    "Android Behaviour Monitor",

                modifier =
                    Modifier.padding(
                        top = 8.dp,
                        bottom = 20.dp
                    )
            )


            // =================================================
            // LIVE DATASET PANEL
            // =================================================

            if (
                datasetRunning
            ) {

                Text(
                    text =
                        "━━━━━━━━━━━━━━━━━━━━"
                )


                Text(
                    text =
                        "DATASET GENERATION"
                )


                Text(
                    text =
                        "━━━━━━━━━━━━━━━━━━━━"
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )


                Text(
                    text =
                        "Dataset: $datasetType"
                )


                Text(
                    text =
                        "Scenario: $currentScenario"
                )


                Text(
                    text =
                        "Run: $currentRun / $totalRuns"
                )


                Text(
                    text =
                        "RUN ID: $currentRunId"
                )


                Text(
                    text =
                        "Stage: $currentStage"
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )


                LinearProgressIndicator(

                    progress = {
                        progressFraction
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        "${(progressFraction * 100).toInt()}% complete"
                )


                Text(
                    text =
                        "Completed: $completedRuns / $totalRuns"
                )


                Text(
                    text =
                        "Remaining: " +
                                (
                                        totalRuns -
                                                completedRuns
                                        )
                                    .coerceAtLeast(0)
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )
            }


            // =================================================
            // 1. START MONITORING
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        fileMonitor?.stop()


                        val run =
                            runManager.createRun(
                                "MANUAL_MONITOR"
                            )


                        fileMonitor = FileMonitor(testDirectory) { window, features ->
                            AndroidTelemetrySender.sendWindow(
                                runId = run.runId,
                                scenario = run.scenario,
                                windowStartMs = window.windowStartMs,
                                windowEndMs = window.windowEndMs,
                                features = features
                            )
                        }


                        fileMonitor?.setRunContext(

                            run.runId,

                            run.scenario,

                            run.telemetryFile
                        )


                        fileMonitor?.start()


                        status =
                            "Monitoring started\n" +
                                    "Run: ${run.runId}"

                    } catch (e: Exception) {

                        status =
                            "Monitoring error: " +
                                    e.message
                    }
                }

            ) {

                Text(
                    "Start Monitoring"
                )
            }


            // =================================================
            // 2. GENERATE DUMMY FILES
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        fileMonitor?.stop()


                        runManager.resetSandbox(
                            testDirectory
                        )


                        val run =
                            runManager.createRun(
                                "SETUP"
                            )


                        val guard =
                            SandboxGuard(
                                testDirectory
                            )


                        val config =
                            SimulatorConfig(

                                fileCount = 20,

                                directoryCount = 5
                            )


                        val builder =
                            DummyTreeBuilder(

                                guard,

                                config
                            )


                        generatedFiles =
                            builder.build()


                        fileMonitor =
                            FileMonitor(
                                testDirectory
                            )


                        fileMonitor?.setRunContext(

                            run.runId,

                            "SETUP",

                            run.telemetryFile
                        )


                        fileMonitor?.start()


                        status =
                            "Created " +
                                    "${generatedFiles.size} " +
                                    "dummy files\n" +
                                    "Run: ${run.runId}"

                    } catch (e: Exception) {

                        status =
                            "Simulator error: " +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "Generate Dummy Files"
                )
            }


            // =================================================
            // 3. B1 BACKUP
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        val run =
                            prepareScenario(
                                "B1_backup_copy"
                            )


                        val simulator =
                            B1BackupSimulator(
                                run.guard
                            )


                        val copied =
                            simulator.run(
                                generatedFiles
                            )


                        status =
                            "B1 completed\n" +
                                    "Copied: " +
                                    "$copied files\n" +
                                    "Run: " +
                                    run.runId

                    } catch (e: Exception) {

                        status =
                            "B1 error: " +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "Run B1 Backup"
                )
            }


            // =================================================
            // 4. B2 ARCHIVE
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        val run =
                            prepareScenario(
                                "B2_archive_compress"
                            )


                        val simulator =
                            B2ArchiveSimulator(
                                run.guard
                            )


                        val processed =
                            simulator.run(
                                generatedFiles
                            )


                        status =
                            "B2 completed\n" +
                                    "Processed: " +
                                    "$processed files\n" +
                                    "Run: " +
                                    run.runId

                    } catch (e: Exception) {

                        status =
                            "B2 error: " +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "Run B2 Archive"
                )
            }


            // =================================================
            // 5. R1 IN-PLACE REWRITE
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        val run =
                            prepareScenario(
                                "R1_inplace_rewrite"
                            )


                        val simulator =
                            R1InplaceRewriteSimulator(
                                run.guard
                            )


                        val changed =
                            simulator.run(
                                generatedFiles
                            )


                        status =
                            "R1 completed\n" +
                                    "Changed: " +
                                    "$changed files\n" +
                                    "Run: " +
                                    run.runId

                    } catch (e: Exception) {

                        status =
                            "R1 error: " +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "Run R1 Rewrite"
                )
            }


            // =================================================
            // 6. R2 COPY + DELETE
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        val run =
                            prepareScenario(
                                "R2_copy_encrypt_delete"
                            )


                        val simulator =
                            R2CopyDeleteSimulator(
                                run.guard
                            )


                        val changed =
                            simulator.run(
                                generatedFiles
                            )


                        status =
                            "R2 completed\n" +
                                    "Changed: " +
                                    "$changed files\n" +
                                    "Run: " +
                                    run.runId

                    } catch (e: Exception) {

                        status =
                            "R2 error: " +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "Run R2 Copy + Delete"
                )
            }


            // =================================================
            // 7. R3 STAGED SLOW
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        val run =
                            prepareScenario(
                                "R3_staged_slow"
                            )


                        status =
                            "R3 running...\n" +
                                    "Run: ${run.runId}"


                        val simulator =
                            R3StagedSlowSimulator(
                                run.guard
                            )


                        simulator.runAsync(

                            generatedFiles,

                            onFinished = {
                                    changed ->

                                runOnUiThread {

                                    status =
                                        "R3 completed\n" +
                                                "Changed: " +
                                                "$changed files\n" +
                                                "Run: " +
                                                run.runId
                                }
                            },

                            onError = {
                                    error ->

                                runOnUiThread {

                                    status =
                                        "R3 error: " +
                                                error.message
                                }
                            }
                        )

                    } catch (e: Exception) {

                        status =
                            "R3 error: " +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "Run R3 Staged Slow"
                )
            }


            // =================================================
            // 8. FINAL DATASET
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    datasetRunning =
                        true

                    datasetType =
                        "FINAL DATASET"

                    currentRun =
                        0

                    totalRuns =
                        95

                    completedRuns =
                        0

                    currentScenario =
                        "-"

                    currentRunId =
                        "-"

                    currentStage =
                        "Starting"


                    status =
                        "FINAL DATASET GENERATION STARTED..."


                    datasetGenerator =
                        DatasetGenerator(

                            sandboxDirectory =
                                testDirectory,

                            runManager =
                                runManager,

                            androidBehaviorWorkload =
                                androidBehaviorWorkload
                        )


                    datasetGenerator?.generateTestDataset(

                        scenarioRuns =
                            mapOf(

                                "B1_backup_copy" to 16,

                                "B2_archive_compress" to 16,

                                "R1_inplace_rewrite" to 21,

                                "R2_copy_encrypt_delete" to 21,

                                "R3_staged_slow" to 21
                            ),

                        clearExisting =
                            false,

                        startRunNumber =
                            1,

                        onProgress = {
                                progress: String ->

                            runOnUiThread {

                                updateDatasetProgress(
                                    progress
                                )
                            }
                        },

                        onFinished = {

                            runOnUiThread {

                                datasetRunning =
                                    false

                                completedRuns =
                                    totalRuns

                                currentStage =
                                    "Completed"

                                status =
                                    "FINAL DATASET GENERATION COMPLETE!\n\n" +
                                            "95 new enriched runs completed."
                            }
                        },

                        onError = {
                                error: Exception ->

                            runOnUiThread {

                                datasetRunning =
                                    false

                                currentStage =
                                    "ERROR"

                                status =
                                    "DATASET GENERATION ERROR:\n\n" +
                                            error.message
                            }
                        }
                    )
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "GENERATE FINAL DATASET (95 RUNS)"
                )
            }


            // =================================================
            // 9. 25K TRAINING DATASET
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    datasetRunning =
                        true

                    datasetType =
                        "TRAINING DATASET"

                    currentRun =
                        0

                    totalRuns =
                        309

                    completedRuns =
                        0

                    currentScenario =
                        "-"

                    currentRunId =
                        "-"

                    currentStage =
                        "Starting"


                    status =
                        "25K TRAINING DATASET GENERATION STARTED...\n\n" +
                                "Target: ~25,000 telemetry events\n\n" +
                                "B1: 17 runs\n" +
                                "B2: 238 runs\n" +
                                "R1: 12 runs\n" +
                                "R2: 10 runs\n" +
                                "R3: 32 runs"


                    datasetGenerator =
                        DatasetGenerator(

                            sandboxDirectory =
                                testDirectory,

                            runManager =
                                runManager,

                            androidBehaviorWorkload =
                                androidBehaviorWorkload
                        )


                    datasetGenerator?.generateTrainingDataset(

                        b1Runs =
                            17,

                        b2Runs =
                            238,

                        r1Runs =
                            12,

                        r2Runs =
                            10,

                        r3Runs =
                            32,

                        startRunNumber =
                            1,

                        onProgress = {
                                progress: String ->

                            runOnUiThread {

                                updateDatasetProgress(
                                    progress
                                )
                            }
                        },

                        onFinished = {

                            runOnUiThread {

                                datasetRunning =
                                    false

                                completedRuns =
                                    totalRuns

                                currentStage =
                                    "Completed"

                                status =
                                    "25K TRAINING DATASET GENERATION COMPLETE!\n\n" +
                                            "309 runs completed.\n\n" +
                                            "B1: 17\n" +
                                            "B2: 238\n" +
                                            "R1: 12\n" +
                                            "R2: 10\n" +
                                            "R3: 32\n\n" +
                                            "Now pull and count the telemetry."
                            }
                        },

                        onError = {
                                error: Exception ->

                            runOnUiThread {

                                datasetRunning =
                                    false

                                currentStage =
                                    "ERROR"

                                status =
                                    "TRAINING DATASET ERROR:\n\n" +
                                            error.message
                            }
                        }
                    )
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "GENERATE 25K TRAINING DATASET"
                )
            }


            // =================================================
            // 10. FINAL UNSEEN TEST DATASET
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    datasetRunning =
                        true

                    datasetType =
                        "UNSEEN TEST DATASET"

                    currentRun =
                        0

                    totalRuns =
                        157

                    completedRuns =
                        0

                    currentScenario =
                        "-"

                    currentRunId =
                        "-"

                    currentStage =
                        "Starting"


                    status =
                        "UNSEEN TEST DATASET GENERATION STARTED...\n\n" +
                                "Target: ~5,000 benign + ~15,000 malicious-like\n\n" +
                                "B1 Backup: 8 runs\n" +
                                "B2 Archive: 120 runs\n" +
                                "R1 Rewrite: 11 runs\n" +
                                "R2 Copy + Delete: 8 runs\n" +
                                "R3 Staged Slow: 10 runs\n\n" +
                                "Total: 157 fresh runs"


                    datasetGenerator =
                        DatasetGenerator(

                            sandboxDirectory =
                                testDirectory,

                            runManager =
                                unseenTestRunManager,

                            androidBehaviorWorkload =
                                androidBehaviorWorkload
                        )


                    datasetGenerator?.generateTestDataset(

                        scenarioRuns =
                            mapOf(

                                // BENIGN
                                "B1_backup_copy" to 8,

                                "B2_archive_compress" to 120,

                                // MALICIOUS-LIKE
                                "R1_inplace_rewrite" to 11,

                                "R2_copy_encrypt_delete" to 8,

                                "R3_staged_slow" to 10
                            ),

                        // IMPORTANT:
                        // This clears ONLY:
                        // RANSCOPE_UNSEEN_TEST/telemetry/
                        //
                        // Therefore the 5 calibration runs
                        // are removed before final generation.
                        //
                        // Existing training/EAP/final telemetry
                        // is NOT touched.
                        clearExisting =
                            true,

                        startRunNumber =
                            1,

                        onProgress = {
                                progress: String ->

                            runOnUiThread {

                                updateDatasetProgress(
                                    progress
                                )
                            }
                        },

                        onFinished = {

                            runOnUiThread {

                                datasetRunning =
                                    false

                                completedRuns =
                                    totalRuns

                                currentStage =
                                    "Completed"

                                status =
                                    "UNSEEN TEST DATASET GENERATION COMPLETE!\n\n" +
                                            "157 fresh runs completed.\n\n" +
                                            "Expected approximately:\n" +
                                            "~5,000 benign events\n" +
                                            "~15,000 malicious-like events\n\n" +
                                            "DO NOT modify this dataset.\n" +
                                            "Pull and validate it before model testing."
                            }
                        },

                        onError = {
                                error: Exception ->

                            runOnUiThread {

                                datasetRunning =
                                    false

                                currentStage =
                                    "ERROR"

                                status =
                                    "UNSEEN TEST DATASET ERROR:\n\n" +
                                            error.message
                            }
                        }
                    )
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "GENERATE UNSEEN TEST DATASET (157 RUNS)"
                )
            }

            // =================================================
// 11. VERIFY UNSEEN ANDROID TELEMETRY
// =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    datasetRunning =
                        true

                    datasetType =
                        "UNSEEN TELEMETRY VERIFICATION"

                    currentRun =
                        0

                    totalRuns =
                        10

                    completedRuns =
                        0

                    currentScenario =
                        "-"

                    currentRunId =
                        "-"

                    currentStage =
                        "Starting"

                    status =
                        "SFIM CALIBRATION STARTED...\\n\\n" +
                                "5 benign + 5 malicious-like runs\\n\\n" +
                                "B1: 2\\n" +
                                "B2: 3\\n" +
                                "R1: 2\\n" +
                                "R2: 1\\n" +
                                "R3: 2"

                    datasetGenerator =
                        DatasetGenerator(

                            sandboxDirectory =
                                testDirectory,

                            runManager =
                                unseenTestRunManager,

                            androidBehaviorWorkload =
                                androidBehaviorWorkload
                        )

                    datasetGenerator?.generateTestDataset(

                        scenarioRuns =
                            linkedMapOf(

                                // BENIGN
                                "B1_backup_copy" to 2,
                                "B2_archive_compress" to 3,

                                // MALICIOUS-LIKE
                                "R1_inplace_rewrite" to 2,
                                "R2_copy_encrypt_delete" to 1,
                                "R3_staged_slow" to 2
                            ),

                        clearExisting =
                            true,

                        startRunNumber =
                            1,

                        onProgress = {
                                progress: String ->

                            runOnUiThread {

                                updateDatasetProgress(
                                    "UNSEEN TEST\n" +
                                            progress
                                )
                            }
                        },

                        onFinished = {

                            runOnUiThread {

                                datasetRunning =
                                    false

                                completedRuns =
                                    totalRuns

                                currentStage =
                                    "Completed"

                                status =
                                    "UNSEEN TELEMETRY VERIFICATION COMPLETE!\n\n" +
                                            "2 fresh runs completed.\n\n" +
                                            "Now pull RANSCOPE_UNSEEN_TEST and inspect\n" +
                                            "events.jsonl + android_events.jsonl."
                            }
                        },

                        onError = {
                                error: Exception ->

                            runOnUiThread {

                                datasetRunning =
                                    false

                                currentStage =
                                    "ERROR"

                                status =
                                    "UNSEEN TELEMETRY VERIFICATION ERROR:\n\n" +
                                            error.message
                            }
                        }
                    )
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "VERIFY UNSEEN TELEMETRY (10 RUNS)"
                )
            }


            // =================================================
            // 11. TEST ANDROID BEHAVIOUR
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        status =
                            "Running Android behaviour test..."


                        androidBehaviorWorkload
                            .runBenignBehavior {

                                runOnUiThread {

                                    status =
                                        "Android behaviour test completed"
                                }
                            }

                    } catch (e: Exception) {

                        status =
                            "Android behaviour error:\n" +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "TEST Android Behavior"
                )
            }


            // =================================================
            // 12. EAP PIPELINE
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    datasetRunning =
                        true

                    datasetType =
                        "EAP DATASET"

                    currentRun =
                        0

                    totalRuns =
                        60

                    completedRuns =
                        0

                    currentScenario =
                        "-"

                    currentRunId =
                        "-"

                    currentStage =
                        "Starting"


                    status =
                        "EAP PIPELINE TEST STARTED..."


                    datasetGenerator =
                        DatasetGenerator(

                            sandboxDirectory =
                                testDirectory,

                            runManager =
                                runManager,

                            androidBehaviorWorkload =
                                androidBehaviorWorkload
                        )


                    datasetGenerator?.generateEapDataset(

                        benignRuns =
                            30,

                        maliciousRuns =
                            30,

                        startRunNumber =
                            1,

                        onProgress = {
                                progress: String ->

                            runOnUiThread {

                                updateDatasetProgress(
                                    progress
                                )
                            }
                        },

                        onFinished = {

                            runOnUiThread {

                                datasetRunning =
                                    false

                                completedRuns =
                                    totalRuns

                                currentStage =
                                    "Completed"

                                status =
                                    "EAP PIPELINE TEST COMPLETE!\n\n" +
                                            "30 benign + 30 malicious-like runs completed."
                            }
                        },

                        onError = {
                                error: Exception ->

                            runOnUiThread {

                                datasetRunning =
                                    false

                                currentStage =
                                    "ERROR"

                                status =
                                    "EAP PIPELINE TEST ERROR:\n\n" +
                                            error.message
                            }
                        }
                    )
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "TEST EAP PIPELINE (60 RUNS)"
                )
            }


            // =================================================
            // 13. CREATE TEST FILE
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    try {

                        if (
                            !testDirectory.exists()
                        ) {

                            testDirectory.mkdirs()
                        }


                        val testFile =
                            File(

                                testDirectory,

                                "test_file.txt"
                            )


                        testFile.writeText(
                            "RANSCOPE test data"
                        )


                        status =
                            "Created test_file.txt"

                    } catch (e: Exception) {

                        status =
                            "Create error: " +
                                    e.message
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )

            ) {

                Text(
                    "Create Test File"
                )
            }


            // =================================================
            // 14. MODIFY TEST FILE
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    val testFile =
                        File(

                            testDirectory,

                            "test_file.txt"
                        )


                    if (
                        testFile.exists()
                    ) {

                        testFile.appendText(
                            "\nFile modified."
                        )


                        status =
                            "Modified test_file.txt"

                    } else {

                        status =
                            "Test file does not exist"
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 10.dp
                    )

            ) {

                Text(
                    "Modify Test File"
                )
            }


            // =================================================
            // 15. DELETE TEST FILE
            // =================================================

            Button(

                enabled =
                    !datasetRunning,

                onClick = {

                    val testFile =
                        File(

                            testDirectory,

                            "test_file.txt"
                        )


                    if (
                        testFile.exists()
                    ) {

                        testFile.delete()


                        status =
                            "Deleted test_file.txt"

                    } else {

                        status =
                            "Test file does not exist"
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 10.dp
                    )

            ) {

                Text(
                    "Delete Test File"
                )
            }


            // =================================================
            // STATUS
            // =================================================

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )


            Text(
                text = status
            )
        }
    }


    // ========================================================
    // PREPARE MANUAL SCENARIO
    // ========================================================

    private fun prepareScenario(
        scenario: String
    ): ScenarioRun {

        fileMonitor?.stop()


        runManager.resetSandbox(
            testDirectory
        )


        val run =
            runManager.createRun(
                scenario
            )


        val guard =
            SandboxGuard(
                testDirectory
            )


        val config =
            SimulatorConfig(

                fileCount = 20,

                directoryCount = 5
            )


        val builder =
            DummyTreeBuilder(

                guard,

                config
            )


        generatedFiles =
            builder.build()


        fileMonitor =
            FileMonitor(
                testDirectory
            )


        fileMonitor?.setRunContext(

            run.runId,

            scenario,

            run.telemetryFile
        )


        fileMonitor?.start()


        return ScenarioRun(

            runId =
                run.runId,

            guard =
                guard
        )
    }


    // ========================================================
    // CLEANUP
    // ========================================================

    override fun onDestroy() {

        fileMonitor?.stop()

        datasetGenerator?.stop()

        super.onDestroy()
    }
}


// ============================================================
// SCENARIO RUN
// ============================================================

data class ScenarioRun(

    val runId: String,

    val guard: SandboxGuard
)