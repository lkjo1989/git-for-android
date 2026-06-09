package com.gitforandroid.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

object StoragePermissionManager {

    /**
     * Check if the app has sufficient storage permissions for the current API level.
     *
     * API 30+ (Android 11+): requires MANAGE_EXTERNAL_STORAGE (checked via Environment.isExternalStorageManager)
     * API < 30: requires WRITE_EXTERNAL_STORAGE
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns the intent that opens the system settings page for granting
     * "All files access" (MANAGE_EXTERNAL_STORAGE) permission.
     * Only needed on API 30+.
     */
    fun getManageStorageIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:com.gitforandroid")
        )
    }

    /**
     * Returns the default clone base directory, preferring a user-accessible
     * public path when storage permission is granted, falling back to the
     * app-private directory (always writable, no permission needed) otherwise.
     */
    fun getDefaultCloneBaseDir(context: Context): String {
        return if (hasStoragePermission(context)) {
            getPublicCloneBaseDir()
        } else {
            getAppPrivateCloneBaseDir(context)
        }
    }

    /**
     * Public directory visible to file managers and other apps.
     * Only usable when MANAGE_EXTERNAL_STORAGE is granted (API 30+)
     * or WRITE_EXTERNAL_STORAGE (API < 30).
     */
    fun getPublicCloneBaseDir(): String {
        val dir = java.io.File(Environment.getExternalStorageDirectory(), "GitRepos")
        dir.mkdirs()
        return dir.absolutePath
    }

    /**
     * App-private external storage — always writable, no runtime permissions
     * required. Files are deleted when the app is uninstalled.
     */
    fun getAppPrivateCloneBaseDir(context: Context): String {
        val externalDir = context.getExternalFilesDir(null)
        if (externalDir != null) {
            val reposDir = java.io.File(externalDir, "repos")
            reposDir.mkdirs()
            return reposDir.absolutePath
        }
        // Fallback to internal storage
        val internalDir = java.io.File(context.filesDir, "repos")
        internalDir.mkdirs()
        return internalDir.absolutePath
    }

    /**
     * Tries to extract a filesystem path from a SAF document tree URI.
     * This works on most devices because the URI usually encodes the storage
     * volume and path. Returns null if the path cannot be determined.
     *
     * URI format examples:
     *   content://com.android.externalstorage.documents/tree/primary%3AGitRepos
     *     → /storage/emulated/0/GitRepos
     *   content://com.android.externalstorage.documents/tree/1A2B-3C4D%3Arepos
     *     → /storage/1A2B-3C4D/repos
     */
    fun extractPathFromTreeUri(uri: Uri): String? {
        return try {
            val docId = uri.path?.substringAfter("/tree/") ?: return null
            val decoded = Uri.decode(docId)

            // Split on ':' to get volume and path parts
            val colonIndex = decoded.indexOf(':')
            if (colonIndex < 0) return null

            val volume = decoded.substring(0, colonIndex)
            val path = decoded.substring(colonIndex + 1)

            if (volume == "primary") {
                "${Environment.getExternalStorageDirectory().absolutePath}/$path"
            } else {
                "/storage/$volume/$path"
            }
        } catch (e: Exception) {
            null
        }
    }
}
