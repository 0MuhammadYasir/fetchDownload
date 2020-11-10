package com.example.fetchdownloadrx

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.annotation.NonNull
import java.io.File
import java.text.DecimalFormat


class Utils {

    private fun Utils() {}

    companion object {

        @NonNull
        fun getMimeType(@NonNull context: Context, @NonNull uri: Uri?): String? {
            val cR: ContentResolver = context.contentResolver
            val mime = MimeTypeMap.getSingleton()
            var type = mime.getExtensionFromMimeType(uri?.let { cR.getType(it) })
            if (type == null) {
                type = "*/*"
            }
            return type
        }

        fun deleteFileAndContents(@NonNull file: File) {
            if (file.exists()) {
                if (file.isDirectory) {
                    val contents: Array<File> = file.listFiles()
                    if (contents != null) {
                        for (content in contents) {
                            deleteFileAndContents(content)
                        }
                    }
                }
                file.delete()
            }
        }

        @NonNull
        fun getETAString(@NonNull context: Context, etaInMilliSeconds: Long): String? {
            if (etaInMilliSeconds < 0) {
                return ""
            }
            var seconds = (etaInMilliSeconds / 1000).toInt()
            val hours = seconds / 3600
            seconds -= hours * 3600
            val minutes = seconds / 60
            seconds -= minutes * 60
            return when {
                hours > 0 -> {
                    context.getString(R.string.download_eta_hrs, hours, minutes, seconds)
                }
                minutes > 0 -> {
                    context.getString(R.string.download_eta_min, minutes, seconds)
                }
                else -> {
                    context.getString(R.string.download_eta_sec, seconds)
                }
            }
        }

        @NonNull
        fun getDownloadSpeedString(
            @NonNull context: Context,
            downloadedBytesPerSecond: Long
        ): String? {
            if (downloadedBytesPerSecond < 0) {
                return ""
            }
            val kb = downloadedBytesPerSecond.toDouble() / 1000.toDouble()
            val mb = kb / 1000.toDouble()
            val decimalFormat = DecimalFormat(".##")
            return when {
                mb >= 1 -> {
                    context.getString(R.string.download_speed_mb, decimalFormat.format(mb))
                }
                kb >= 1 -> {
                    context.getString(R.string.download_speed_kb, decimalFormat.format(kb))
                }
                else -> {
                    context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond)
                }
            }
        }

        @NonNull
        fun createFile(filePath: String?): File? {
            val file = File(filePath)
            if (!file.exists()) {
                val parent: File = file.parentFile
                if (!parent.exists()) {
                    parent.mkdirs()
                }
                file.createNewFile()
            }
            return file
        }

        fun getProgress(downloaded: Long, total: Long): Int {
            return when {
                total < 1 -> {
                    -1
                }
                downloaded < 1 -> {
                    0
                }
                downloaded >= total -> {
                    100
                }
                else -> {
                    (downloaded.toDouble() / total.toDouble() * 100).toInt()
                }
            }
        }
    }
}