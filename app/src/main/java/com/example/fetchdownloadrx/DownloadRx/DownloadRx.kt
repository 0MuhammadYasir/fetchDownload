package com.example.fetchdownloadrx.DownloadRx

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArrayMap
import com.example.fetchdownloadrx.Data
import com.example.fetchdownloadrx.R
import com.google.android.material.snackbar.Snackbar
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import com.tonyodev.fetch2rx.RxFetch
import io.reactivex.disposables.Disposable
import org.jetbrains.annotations.NotNull


class DownloadRx : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 400
    private val groupId = 12

    private val fileProgressMap: ArrayMap<Int, Int> = ArrayMap()
    private lateinit var rxFetch: RxFetch
    @Nullable
    private lateinit var enqueueDisposable: Disposable
    @Nullable
    private lateinit var resumeDisposable: Disposable

    private lateinit var progressTextView: TextView
    private lateinit var labelTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button
    private lateinit var mainView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_rx)
        setUpViews()
        rxFetch = RxFetch.Impl.getDefaultRxInstance()
        reset()
    }

    private fun setUpViews() {
        progressTextView = findViewById<TextView>(R.id.progressTextView)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)
        startButton = findViewById<Button>(R.id.startButton)
        labelTextView = findViewById<TextView>(R.id.labelTextView)
        mainView = findViewById<View>(R.id.activity_loading)
        startButton.setOnClickListener { v ->
            val label = startButton.text as String
            val context: Context = this
            if (label == context.getString(R.string.reset)) {
                rxFetch.deleteAll()
                reset()
            } else {
                startButton.visibility = View.GONE
                labelTextView.text = context.getString(R.string.fetch_started)
                checkStoragePermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rxFetch.addListener(fetchListener)
        resumeDisposable =
            rxFetch.getDownloadsInGroup(groupId).flowable.subscribe({ downloads: List<Download> ->
                for (download in downloads) {
                    if (fileProgressMap.containsKey(download.id)) {
                        fileProgressMap[download.id] = download.progress
                        updateUIWithProgress()
                    }
                }
            }) { throwable: Throwable? ->
//                val error: Error = FetchErrorUtils.getErrorFromThrowable(throwable)
                Log.e("GamesFilesActivity Error: %1\$s", "onResume:  ${throwable?.message}")
            }
    }

    override fun onPause() {
        super.onPause()
        rxFetch.removeListener(fetchListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        rxFetch.deleteAll()
        rxFetch.close()
        if (enqueueDisposable != null && !enqueueDisposable.isDisposed) {
            enqueueDisposable.dispose()
        }
        if (resumeDisposable != null && !resumeDisposable.isDisposed) {
            resumeDisposable.dispose()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            enqueueFiles()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE || grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueFiles()
        } else {
            Toast.makeText(this, R.string.permission_not_enabled, Toast.LENGTH_SHORT).show()
            reset()
        }
    }

    private fun updateUIWithProgress() {
        val totalFiles: Int = fileProgressMap.size
        val completedFiles: Int = getCompletedFileCount()
        progressTextView.text = resources.getString(
            R.string.complete_over,
            completedFiles,
            totalFiles
        )
        val progress: Int = getDownloadProgress()
        progressBar.progress = progress
        if (completedFiles == totalFiles) {
            labelTextView.text = getString(R.string.fetch_done)
            startButton.text = applicationContext.getString(R.string.reset)
            startButton.visibility = View.VISIBLE
        }
    }

    private fun getDownloadProgress(): Int {
        var currentProgress = 0
        val totalProgress: Int = fileProgressMap.size * 100
        val ids: Set<Int> = fileProgressMap.keys
        for (id in ids) {
            currentProgress += fileProgressMap[id]!!
        }
        currentProgress = (currentProgress.toDouble() / totalProgress.toDouble() * 100).toInt()
        return currentProgress
    }

    private fun getCompletedFileCount(): Int {
        var count = 0
        val ids: Set<Int> = fileProgressMap.keys
        for (id in ids) {
            val progress = fileProgressMap[id]!!
            if (progress == 100) {
                count++
            }
        }
        return count
    }

    private fun reset() {
        rxFetch.deleteAll()
        fileProgressMap.clear()
        progressBar.progress = 0
        progressTextView.text = ""
        labelTextView.setText(R.string.start_fetching)
        startButton.setText(R.string.start)
        startButton.visibility = View.VISIBLE
    }

    private fun enqueueFiles() {
        val requestList: List<Request> = Data.getGameUpdates()
        for (request in requestList) {
            request.groupId = groupId
        }
        enqueueDisposable = rxFetch.enqueue(requestList).flowable.subscribe({ updatedRequests ->
            for (request in updatedRequests) {
                fileProgressMap[request.first.id] = 0
                updateUIWithProgress()
            }
        }) { throwable ->
            Log.e("GamesFilesActivity Error: %1\$s", "enqueueFiles: ${throwable.message}")
        }
    }

    private val fetchListener: FetchListener = object : AbstractFetchListener() {
        override fun onCompleted(@NotNull download: Download) {
            fileProgressMap[download.id] = download.progress
            updateUIWithProgress()
        }

        override fun onError(
            download: Download,
            error: Error,
            throwable: Throwable?
        ) {
            super.onError(download, error, throwable)
            reset()
            Snackbar.make(mainView, R.string.game_download_error, Snackbar.LENGTH_INDEFINITE).show()
        }

        override fun onProgress(
            @NotNull download: Download,
            etaInMilliseconds: Long,
            downloadedBytesPerSecond: Long
        ) {
            super.onProgress(download, etaInMilliseconds, downloadedBytesPerSecond)
            fileProgressMap[download.id] = download.progress
            updateUIWithProgress()
        }
    }

}