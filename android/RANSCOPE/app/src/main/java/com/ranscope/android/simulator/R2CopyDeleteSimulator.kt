package com.ranscope.android.simulator

import java.io.File
import kotlin.random.Random

class R2CopyDeleteSimulator(
    private val guard: SandboxGuard
) {

    private val random =
        Random(System.currentTimeMillis())

    fun run(files: List<File>): Int {

        val targetExtensions = setOf(
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
         * R2 targets approximately 90% of
         * eligible files, matching the
         * original simulator.
         */
        val targets = files.filter { file ->

            file.exists() &&
                    targetExtensions.contains(
                        "." + file.extension.lowercase()
                    ) &&
                    random.nextDouble() < 0.90
        }

        var changedFiles = 0

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

        for (sourceFile in targets) {

            guard.check(sourceFile)

            /*
             * Read the original file.
             */
            val originalData =
                sourceFile.readBytes()

            /*
             * Safely scramble the contents.
             */
            val scrambledData =
                scramble(
                    originalData,
                    key
                )

            /*
             * Create:
             *
             * sample_1.pdf
             *
             *       ↓
             *
             * sample_1.pdf.enc
             */
            val encryptedCopy =
                File(
                    sourceFile.parentFile,
                    sourceFile.name + ".enc"
                )

            guard.check(encryptedCopy)

            /*
             * Write the scrambled copy.
             */
            encryptedCopy.writeBytes(
                scrambledData
            )

            /*
             * Delete the original.
             */
            if (sourceFile.delete()) {
                changedFiles++
            }
        }

        return changedFiles
    }

    /*
     * Simple reversible byte scrambling.
     *
     * The same operation with the same key
     * can reverse the transformation.
     *
     * This is deliberately NOT real encryption.
     */
    private fun scramble(
        data: ByteArray,
        key: Int
    ): ByteArray {

        val output =
            ByteArray(data.size)

        var state =
            key.toLong()

        for (i in data.indices) {

            state =
                (
                        state * 1103515245L +
                                12345L
                        ) and 0x7FFFFFFFL

            val mask =
                (state and 0xFF).toInt()

            output[i] =
                (
                        data[i].toInt() xor mask
                        ).toByte()
        }

        return output
    }
}