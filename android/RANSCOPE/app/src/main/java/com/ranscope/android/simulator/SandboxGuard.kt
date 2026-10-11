package com.ranscope.android.simulator

import java.io.File

class SandboxGuard(
    private val sandboxRoot: File
) {

    init {
        sandboxRoot.mkdirs()
    }

    fun root(): File {
        return sandboxRoot
    }

    fun check(file: File) {
        val rootPath = sandboxRoot.canonicalFile
        val filePath = file.canonicalFile

        if (filePath != rootPath &&
            !filePath.toPath().startsWith(rootPath.toPath())
        ) {
            throw SecurityException(
                "SANDBOX VIOLATION: $filePath is outside $rootPath"
            )
        }
    }

    fun safeFile(relativePath: String): File {
        val file = File(sandboxRoot, relativePath)
        check(file)
        return file
    }

    fun safeDirectory(relativePath: String): File {
        val directory = File(sandboxRoot, relativePath)
        check(directory)
        return directory
    }
}