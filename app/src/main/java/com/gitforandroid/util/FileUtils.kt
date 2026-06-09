package com.gitforandroid.util

import java.io.File

object FileUtils {

    fun ensureDir(path: String): File {
        val dir = File(path)
        dir.mkdirs()
        return dir
    }

    fun listFiles(path: String, recursive: Boolean = false): List<File> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return if (recursive) {
            dir.walkTopDown().filter { it.isFile }.toList()
        } else {
            dir.listFiles()?.filter { it.isFile } ?: emptyList()
        }
    }

    fun getRelativePath(basePath: String, file: File): String {
        val base = File(basePath).canonicalPath
        val filePath = file.canonicalPath
        return if (filePath.startsWith(base)) {
            filePath.removePrefix(base).removePrefix("/")
        } else filePath
    }

    fun humanReadableSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return "%.1f %s".format(size, units[unitIndex])
    }
}
