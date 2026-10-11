package com.ranscope.android.simulator

import java.io.File
import kotlin.random.Random

class DummyTreeBuilder(
    private val guard: SandboxGuard,
    private val config: SimulatorConfig
) {

    private val random = Random(config.seed)

    private val directoryNames = listOf(
        "Documents",
        "Pictures",
        "Downloads",
        "Projects",
        "Notes",
        "Reports",
        "Work",
        "Scans",
        "Books",
        "Camera"
    )

    private val extensions = listOf(
        ".txt",
        ".md",
        ".csv",
        ".json",
        ".jpg",
        ".png",
        ".pdf",
        ".docx"
    )

    fun build(): List<File> {

        val root = guard.root()

        val directories = mutableListOf<File>()

        // Create directories
        repeat(config.directoryCount) {

            val name =
                directoryNames[random.nextInt(directoryNames.size)] +
                        "_" +
                        random.nextInt(1, 999)

            val directory = File(root, name)

            guard.check(directory)
            directory.mkdirs()

            directories.add(directory)
        }

        val files = mutableListOf<File>()

        // Create dummy files
        repeat(config.fileCount) {

            val directory = directories[
                random.nextInt(directories.size)
            ]

            val extension =
                extensions[random.nextInt(extensions.size)]

            val file = File(
                directory,
                "sample_${it}$extension"
            )

            guard.check(file)

            val size = random.nextInt(
                2_048,
                16_384
            )

            val content = ByteArray(size)

            random.nextBytes(content)

            file.writeBytes(content)

            files.add(file)
        }

        return files
    }
}