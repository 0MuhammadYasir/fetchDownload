package com.example.fetchdownloadrx.DownloadList

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fetchdownloadrx.Data
import com.example.fetchdownloadrx.R
import com.google.android.material.snackbar.Snackbar
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.Fetch.Impl.getInstance
import com.tonyodev.fetch2core.Downloader.FileDownloaderType
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import org.jetbrains.annotations.NotNull
import kotlin.collections.ArrayList


class DownloadListActivity : AppCompatActivity(), ActionListener {

    private val STORAGE_PERMISSION_CODE = 200
    private val UNKNOWN_REMAINING_TIME: Long = -1
    private val UNKNOWN_DOWNLOADED_BYTES_PER_SECOND: Long = 0
    private val GROUP_ID = "listGroup".hashCode()
    val FETCH_NAMESPACE = "DownloadListActivity"

    private lateinit var mainView: View
    private lateinit var fileAdapter: FileAdapter
    private lateinit var fetch: Fetch


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.download_list)
        setUpViews()
        val fetchConfiguration = FetchConfiguration.Builder(this)
            .setDownloadConcurrentLimit(4)
            .setHttpDownloader(OkHttpDownloader(FileDownloaderType.PARALLEL))
            .setNamespace(FETCH_NAMESPACE)
            .setNotificationManager(object : DefaultFetchNotificationManager(this) {
                @NotNull
                override fun getFetchInstanceForNamespace(@NotNull namespace: String): Fetch {
                    return fetch
                }
            })
            .build()
        fetch = getInstance(fetchConfiguration)
        checkStoragePermissions()
    }

    private fun setUpViews() {
        val networkSwitch = findViewById<SwitchCompat>(R.id.networkSwitch)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        mainView = findViewById(R.id.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this)
        networkSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                fetch.setGlobalNetworkType(NetworkType.WIFI_ONLY)
            } else {
                fetch.setGlobalNetworkType(NetworkType.ALL)
            }
        }
        fileAdapter = FileAdapter(this)
        recyclerView.adapter = fileAdapter
    }

    override fun onResume() {
        super.onResume()

        fetch.getDownloadsInGroup(GROUP_ID) { downloads: List<Download> ->
            val list = ArrayList(downloads)
            for (download in list) {
                download?.let { fileAdapter.addDownload(it)}
            }
        }.addListener(fetchListener)
    }

    override fun onPause() {
        super.onPause()
        fetch.removeListener(fetchListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        fetch.close()
    }

    private val fetchListener: FetchListener = object : AbstractFetchListener() {
        override fun onAdded(download: Download) {
            fileAdapter.addDownload(download)
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }

        override fun onCompleted(download: Download) {
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }

        override fun onError(download: Download, error: Error, @Nullable throwable: Throwable?) {
            super.onError(download, error, throwable)
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }

        override fun onProgress(
            download: Download,
            etaInMilliseconds: Long,
            downloadedBytesPerSecond: Long
        ) {
            fileAdapter.update(download, etaInMilliseconds, downloadedBytesPerSecond)
        }

        override fun onPaused(download: Download) {
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }

        override fun onResumed(download: Download) {
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }

        override fun onCancelled(download: Download) {
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }

        override fun onRemoved(download: Download) {
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }

        override fun onDeleted(download: Download) {
            fileAdapter.update(
                download,
                UNKNOWN_REMAINING_TIME,
                UNKNOWN_DOWNLOADED_BYTES_PER_SECOND
            )
        }
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            enqueueDownloads()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownloads()
        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_INDEFINITE)
                .show()
        }
    }

    private fun enqueueDownloads() {
        val requests = Data.getFetchRequestWithGroupId(GROUP_ID)
        requests?.let {
            fetch.enqueue(it) {

            }
        }
    }

    override fun onPauseDownload(id: Int) {
        fetch.pause(id)
    }

    override fun onResumeDownload(id: Int) {
        fetch.resume(id)
    }

    override fun onRemoveDownload(id: Int) {
        fetch.remove(id)
    }

    override fun onRetryDownload(id: Int) {
        fetch.retry(id)
    }
}