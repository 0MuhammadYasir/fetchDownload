package com.example.fetchdownloadrx

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.Downloader.FileDownloaderType
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.MutableExtras
import com.tonyodev.fetch2core.Reason
import org.jetbrains.annotations.NotNull


class SingleDownloadActivity : AppCompatActivity(), FetchObserver<Download> {

    private val STORAGE_PERMISSION_CODE = 100
    private lateinit var fetch: Fetch
    private lateinit var request: Request
    private lateinit var mainView: View
    private val TAG = "SingleDownloadActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_download)
        mainView = findViewById<View>(R.id.activity_single_download)

        val fetchConfiguration = FetchConfiguration.Builder(this)
            .enableRetryOnNetworkGain(true)
            .setDownloadConcurrentLimit(3)
            .setHttpDownloader(HttpUrlConnectionDownloader(FileDownloaderType.PARALLEL)) // OR
            //.setHttpDownloader(getOkHttpDownloader())
            .build()
        Fetch.Impl.setDefaultInstanceConfiguration(fetchConfiguration)

        fetch = Fetch.Impl.getDefaultInstance()
        checkStoragePermission()
    }

    override fun onResume() {
        super.onResume()
        if (this::request.isInitialized){
            fetch.attachFetchObserversForDownload(request.id, this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::request.isInitialized) {
            fetch.removeFetchObserversForDownload(request.id, this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fetch.close()
    }


    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            enqueueDownload()
        }
    }

    private fun enqueueDownload() {
        val url: String = Data.sampleUrls[5]
        val filePath: String = Data.getSaveDir().toString() + "/movies/" + Data.getNameFromUrl(url)
        Log.e(TAG, "enqueueDownload: $filePath")
        request = Request(url, filePath)
        request.extras = getExtrasForRequest(request)
        fetch.attachFetchObserversForDownload(request.id, this)
            .enqueue(request,
                { result -> request = result },
                { result ->
                    Log.e("TAG", "enqueueDownload: ${result.throwable.toString()}")
                })
        fetch.attachFetchObserversForDownload(request.id)
    }

    private fun getExtrasForRequest(request: Request): Extras {
        val extras = MutableExtras()
        extras.putBoolean("testBoolean", true)
        extras.putString("testString", "test")
        extras.putFloat("testFloat", Float.MIN_VALUE)
        extras.putDouble("testDouble", Double.MIN_VALUE)
        extras.putInt("testInt", Int.MAX_VALUE)
        extras.putLong("testLong", Long.MAX_VALUE)
        return extras
    }

    override fun onChanged(data: Download, reason: Reason) {
        updateViews(data, reason);
    }

    private fun updateViews(@NotNull download: Download, reason: Reason) {
        if (request.id == download.id) {
            if (reason === Reason.DOWNLOAD_QUEUED || reason === Reason.DOWNLOAD_COMPLETED) {
                setTitleView(download.file)
            }
            setProgressView(download.status, download.progress)
            findViewById<TextView>(R.id.etaTextView).text =
                Utils.getETAString(this, download.etaInMilliSeconds)
            findViewById<TextView>(R.id.downloadSpeedTextView).text =
                Utils.getDownloadSpeedString(this, download.downloadedBytesPerSecond)

            if (download.error != Error.NONE) {
                showDownloadErrorSnackBar(download.error)
            }
        }
    }

    private fun setTitleView(@NonNull fileName: String) {
        val uri: Uri = Uri.parse(fileName)
        findViewById<TextView>(R.id.titleTextView).text = uri.lastPathSegment
    }

    private fun setProgressView(@NonNull status: Status, progress: Int) {
        var tvProgress = findViewById<TextView>(R.id.progressTextView)
        when (status) {
            Status.QUEUED -> tvProgress.text = resources.getString(R.string.queued)
            Status.ADDED -> tvProgress.text = resources.getString(R.string.added)
            Status.DOWNLOADING ->
                if (progress == -1)
                    tvProgress.text = resources.getString(R.string.downloading)
                else {
                    val progressString = resources.getString(R.string.percent_progress, progress);
                    tvProgress.text = progressString
                }

            Status.COMPLETED ->
                if (progress == -1) {
                    tvProgress.text = resources.getString(R.string.downloading)
                } else {
                    val progressString = resources.getString(R.string.percent_progress, progress);
                    tvProgress.text = progressString
                }

            else -> tvProgress.text = resources.getString(R.string.status_unknown)

        }
    }

    private fun showDownloadErrorSnackBar(@NotNull error: Error) {
        val snackbar = Snackbar.make(
            mainView,
            "Download Failed: ErrorCode: $error",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction(R.string.retry) {
            fetch.retry(request.id);
            snackbar.dismiss();
        }
        snackbar.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownload();
        } else Toast.makeText(applicationContext, "permission_not_enabled", Toast.LENGTH_LONG)
            .show()
    }
}