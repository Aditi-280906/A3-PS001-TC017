package com.ranscope.android.simulator

import java.io.File
import kotlin.random.Random

class R3StagedSlowSimulator(
    private val guard: SandboxGuard
) {

    private val random =
        Random(System.currentTimeMillis())

    private val targetExtensions = setOf(
        ".txt",
        ".md",
        ".csv",
        ".json",
        ".jpg",
        ".png",
        ".pdf",
        ".docx",
        ".xlsx",
        ".db"
    )

    /*
     * R3 runs the workload in stages.
     *
     * The callback is called when the workload
     * finishes so MainActivity can update the UI.
     */
    fun runAsync(
        files: List<File>,
        onFinished: (Int) -> Unit,
        onError: (Exception) -> Unit
    ) {

        Thread {

            try {

                val changedFiles =
                    runInternal(files)

                onFinished(
                    changedFiles
                )

            } catch (e: Exception) {

                onError(e)
            }

        }.start()
    }

    private fun runInternal(
        files: List<File>
    ): Int {

        /*
         * Safe reversible scrambling key.
         *
         * This is NOT real encryption.
         */
        val key =
            random.nextInt(
                1,
                Int.MAX_VALUE
            )

        // ---------------------------------------------------------
        // PHASE 1 — RECONNAISSANCE
        // ---------------------------------------------------------

        /*
         * Visit every directory first.
         *
         * This intentionally creates a quieter
         * reconnaissance period before file rewriting.
         */
        val directories =
            files
                .mapNotNull {
                    it.parentFile
                }
                .distinctBy {
                    it.absolutePath
                }
                .sortedBy {
                    it.absolutePath
                }

        for (directory in directories) {

            guard.check(directory)

            /*
             * Reading the directory itself does not
             * modify user data.
             */
            directory.listFiles()

            /*
             * Small real delay so the telemetry has
             * a staged/slow character.
             */
            Thread.sleep(
                random.nextLong(
                    300L,
                    1000L
                )
            )
        }

        // ---------------------------------------------------------
        // PHASE 2 — FILE INSPECTION
        // ---------------------------------------------------------

        for ((index, file) in files.withIndex()) {

            if (!file.exists()) {
                continue
            }

            guard.check(file)

            /*
             * Inspect every file.
             */
            file.length()

            /*
             * Every third eligible file gets a small
             * read, matching the original simulator's
             * header-sniffing behavior.
             */
            if (
                index % 3 == 0 &&
                targetExtensions.contains(
                    "." + file.extension.lowercase()
                )
            ) {

                file.inputStream().use { input ->

                    val buffer =
                        ByteArray(
                            minOf(
                                256,
                                file.length().toInt()
                            )
                        )

                    if (buffer.isNotEmpty()) {
                        input.read(buffer)
                    }
                }
            }

            Thread.sleep(
                random.nextLong(
                    100L,
                    500L
                )
            )
        }

        // ---------------------------------------------------------
        // PHASE 3 — SELECT TARGETS
        // ---------------------------------------------------------

        /*
         * Original R3 selects approximately
         * 35–60% of eligible files.
         */
        val targetFraction =
            random.nextDouble(
                0.35,
                0.60
            )

        val targets =
            files.filter { file ->

                file.exists() &&
                        targetExtensions.contains(
                            "." + file.extension.lowercase()
                        ) &&
                        random.nextDouble() <
                        targetFraction
            }

        // ---------------------------------------------------------
        // PHASE 4 — DECOY FILE
        // ---------------------------------------------------------

        val decoyFile =
            File(
                guard.root(),
                ".sync_cache.log"
            )

        guard.check(
            decoyFile
        )

        /*
         * Initial decoy write.
         */
        decoyFile.writeText(
            "init\n"
        )

        var changedFiles = 0

        // ---------------------------------------------------------
        // PHASE 5 — SLOW BATCHED REWRITING
        // ---------------------------------------------------------

        var index = 0

        while (index < targets.size) {

            /*
             * Original R3 processes a small batch
             * of approximately 4–9 files.
             */
            val batchSize =
                random.nextInt(
                    4,
                    10
                )

            val end =
                minOf(
                    index + batchSize,
                    targets.size
                )

            val batch =
                targets.subList(
                    index,
                    end
                )

            for (sourceFile in batch) {

                if (!sourceFile.exists()) {
                    continue
                }

                guard.check(
                    sourceFile
                )

                /*
                 * Read the original file.
                 */
                val originalData =
                    sourceFile.readBytes()

                /*
                 * Add a decoy write before
                 * modifying the real target.
                 */
                decoyFile.appendText(
                    generateDecoyText() + "\n"
                )

                /*
                 * Safe reversible scrambling.
                 *
                 * NOT real encryption.
                 */
                val scrambledData =
                    scramble(
                        originalData,
                        key
                    )

                /*
                 * Rewrite the original file
                 * in place.
                 */
                sourceFile.writeBytes(
                    scrambledData
                )

                changedFiles++

                /*
                 * R3 only renames some files.
                 *
                 * Original behavior uses approximately
                 * 50% probability.
                 */
                if (
                    random.nextDouble() < 0.50 &&
                    sourceFile.exists()
                ) {

                    val renamedFile =
                        File(
                            sourceFile.parentFile,
                            sourceFile.name +
                                    "." +
                                    random.nextInt(
                                        100,
                                        1000
                                    )
                        )

                    guard.check(
                        renamedFile
                    )

                    sourceFile.renameTo(
                        renamedFile
                    )
                }
            }

            index = end

            /*
             * Real pause between batches.
             *
             * This is intentionally shorter than the
             * original 8–25 second range so testing
             * the Android app isn't unnecessarily slow.
             *
             * The behavior remains staged and bursty.
             */
            if (index < targets.size) {

                Thread.sleep(
                    random.nextLong(
                        3000L,
                        7000L
                    )
                )
            }
        }

        return changedFiles
    }

    private fun generateDecoyText(): String {

        val words =
            listOf(
                "sync",
                "cache",
                "update",
                "metadata",
                "index",
                "temporary",
                "status",
                "backup"
            )

        return buildString {

            repeat(20) {

                append(
                    words.random(random)
                )

                append(" ")
            }
        }
    }

    /*
     * Safe reversible scrambling.
     *
     * This is deliberately NOT real encryption.
     */
    private fun scramble(
        data: ByteArray,
        key: Int
    ): ByteArray {

        val output =
            ByteArray(
                data.size
            )

        var state =
            key.toLong()

        for (i in data.indices) {

            state =
                (
                        state * 1103515245L +
                                12345L
                        ) and 0x7FFFFFFFL

            val mask =
                (
                        state and 0xFF
                        ).toInt()

            output[i] =
                (
                        data[i].toInt() xor mask
                        ).toByte()
        }

        return output
    }
}