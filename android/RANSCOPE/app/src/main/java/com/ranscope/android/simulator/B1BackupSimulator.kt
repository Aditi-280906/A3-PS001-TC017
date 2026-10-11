package com.ranscope.android.simulator

import java.io.File
import kotlin.random.Random

class B1BackupSimulator(
    private val guard: SandboxGuard
) {

    private val random = Random(System.currentTimeMillis())

    fun run(files: List<File>): Int {

        val root = guard.root()

        // Backup directory stays INSIDE RANSCOPE_TEST
        val backupDirectory = File(
            root,
            "Backup_${random.nextInt(1000, 9999)}"
        )

        guard.check(backupDirectory)
        backupDirectory.mkdirs()

        var copiedFiles = 0

        for (sourceFile in files) {

            // Skip files that don't exist
            if (!sourceFile.exists()) {
                continue
            }

            // Randomly skip some files.
            // This makes B1 less identical between runs.
            if (random.nextDouble() < 0.20) {
                continue
            }

            val relativePath = sourceFile
                .relativeTo(root)

            val destinationFile = File(
                backupDirectory,
                relativePath.path
            )

            // Safety checks BEFORE creating anything
            guard.check(sourceFile)
            guard.check(destinationFile)

            destinationFile.parentFile?.mkdirs()

            sourceFile.copyTo(
                destinationFile,
                overwrite = true
            )

            copiedFiles++
        }

        return copiedFiles
    }
}