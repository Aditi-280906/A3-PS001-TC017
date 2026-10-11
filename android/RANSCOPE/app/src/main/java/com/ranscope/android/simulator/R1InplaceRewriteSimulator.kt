package com.ranscope.android.simulator

import java.io.File
import kotlin.random.Random

class R1InplaceRewriteSimulator(
    private val guard: SandboxGuard
) {

    private val random = Random(System.currentTimeMillis())

    fun run(files: List<File>): Int {

        val root = guard.root()

        // Only use files with extensions that resemble
        // normal user documents/media.
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

        val targets = files.filter { file ->

            file.exists() &&
                    targetExtensions.contains(
                        file.extension.lowercase().let {
                            if (it.isEmpty()) "" else ".$it"
                        }
                    ) &&
                    random.nextDouble() < 0.95
        }

        var changedFiles = 0

        // Random key for the reversible scrambling operation.
        val key = random.nextInt(1, Int.MAX_VALUE)

        for (sourceFile in targets) {

            guard.check(sourceFile)

            // Read the original content.
            val originalData =
                sourceFile.readBytes()

            // Safe reversible scrambling.
            val scrambledData =
                scramble(
                    originalData,
                    key
                )

            // Write back to the SAME file.
            sourceFile.writeBytes(
                scrambledData
            )

            // Rename:
            // example.txt → example.txt.locked
            val lockedFile = File(
                sourceFile.parentFile,
                sourceFile.name + ".locked"
            )

            guard.check(lockedFile)

            if (sourceFile.renameTo(lockedFile)) {
                changedFiles++
            }
        }

        return changedFiles
    }

    private fun scramble(
        data: ByteArray,
        key: Int
    ): ByteArray {

        val output =
            ByteArray(data.size)

        var state = key.toLong()

        for (i in data.indices) {

            // Simple reversible pseudo-random byte stream.
            state =
                (state * 1103515245L + 12345L) and 0x7FFFFFFFL

            val mask =
                (state and 0xFF).toInt()

            output[i] =
                (data[i].toInt() xor mask).toByte()
        }

        return output
    }
}