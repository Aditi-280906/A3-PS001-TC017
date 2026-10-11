package com.ranscope.android.simulator

import java.io.File
import java.io.FileOutputStream
import java.util.zip.Deflater
import kotlin.random.Random

class B2ArchiveSimulator(
    private val guard: SandboxGuard
) {

    private val random = Random(System.currentTimeMillis())

    fun run(files: List<File>): Int {

        val root = guard.root()

        // Create one archive inside the sandbox.
        val archiveFile = File(
            root,
            "archive_${random.nextInt(1000, 9999)}.zipsim"
        )

        guard.check(archiveFile)

        var processedFiles = 0

        // Randomly choose one compression level,
        // just like the original simulator.
        val compressionLevel = listOf(
            1,
            6,
            9
        ).random()

        FileOutputStream(
            archiveFile,
            false
        ).use { output ->

            // Small header identifying our safe simulated archive.
            output.write(
                byteArrayOf(
                    'A'.code.toByte(),
                    'R'.code.toByte(),
                    'C'.code.toByte(),
                    'S'.code.toByte(),
                    'I'.code.toByte(),
                    'M'.code.toByte(),
                    0,
                    1
                )
            )

            // Original simulator processes about 85% of files.
            val selectedFiles =
                files.filter {
                    random.nextDouble() < 0.85
                }

            for ((index, sourceFile) in selectedFiles.withIndex()) {

                if (!sourceFile.exists()) {
                    continue
                }

                guard.check(sourceFile)

                val data = sourceFile.readBytes()

                val compressedData =
                    compress(
                        data,
                        compressionLevel
                    )

                // Store simple metadata before the compressed payload.
                val header =
                    "${sourceFile.name}|${data.size}|${compressedData.size}|$compressionLevel\n"
                        .toByteArray()

                output.write(header)

                // Store compressed data.
                output.write(compressedData)

                processedFiles++

                // Every 10 files, add a small index entry.
                if ((index + 1) % 10 == 0) {

                    val indexText =
                        "index:${selectedFiles
                            .subList(
                                maxOf(0, index - 9),
                                index + 1
                            )
                            .joinToString(",") {
                                it.name
                            }}\n"

                    output.write(
                        indexText.toByteArray()
                    )
                }
            }
        }

        return processedFiles
    }

    private fun compress(
        data: ByteArray,
        level: Int
    ): ByteArray {

        val deflater = Deflater(level)

        return try {

            deflater.setInput(data)
            deflater.finish()

            val output = ByteArray(
                data.size + 1024
            )

            val compressedSize =
                deflater.deflate(output)

            output.copyOf(compressedSize)

        } finally {

            deflater.end()
        }
    }
}