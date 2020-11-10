package com.example.fetchdownloadrx

import android.net.Uri
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import androidx.annotation.NonNull
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request


class Data {

    companion object {

        val sampleUrls = arrayOf(
            "http://pub.quran.digital/text_books/alMokhtasar/alMokhtasar.zip",
            "https://download.blender.org/peach/bigbuckbunny_movies/big_buck_bunny_720p_stereo.avi",
            "http://media.mongodb.org/zips.json",
            "http://www.exampletonyotest/some/unknown/123/Errorlink.txt",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Android_logo_2019.svg/687px-Android_logo_2019.svg.png",
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        )

        private fun Data() {}

        @NonNull
        private fun getFetchRequests(): MutableList<Request> {
            val requests: MutableList<Request> = ArrayList()
            for (sampleUrl in sampleUrls) {
                val request = getFilePath(sampleUrl)?.let { Request(sampleUrl, it) }
                if (request != null) {
                    requests.add(request)
                }
            }
            return requests
        }

        @NonNull
        fun getFetchRequestWithGroupId(groupId: Int): List<Request>? {
            val requests: MutableList<Request> = getFetchRequests()
            for (request in requests) {
                request.groupId = groupId
            }
            return requests
        }

        @NonNull
        private fun getFilePath(@NonNull url: String): String? {
            val uri: Uri = Uri.parse(url)
            val fileName: String? = uri.lastPathSegment
            val dir = getSaveDir()
            return "$dir/DownloadList/$fileName"
        }

        @NonNull
        fun getNameFromUrl(url: String?): String? {
            return Uri.parse(url).lastPathSegment
        }

        @NonNull
        fun getGameUpdates(): MutableList<Request> {
            val requests: MutableList<Request> = ArrayList()
            val url = "http://speedtest.ftp.otenet.gr/files/test100k.db"
            for (i in 0..9) {
                val filePath = getSaveDir() + "/gameAssets/" + "asset_" + i + ".asset"
                val request = Request(url, filePath)
                request.priority = Priority.HIGH
                requests.add(request)
            }
            return requests
        }

        @NonNull
        fun getSaveDir(): String {
            Log.e("DATA" , "getSaveDir: ${getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString()}")
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString().toString() + "/fetch"
        }

    }
}