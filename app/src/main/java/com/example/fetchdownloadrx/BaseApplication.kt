package com.example.fetchdownloadrx

import android.app.Application
import com.tonyodev.fetch2.Fetch.Impl.setDefaultInstanceConfiguration
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2core.Downloader.FileDownloaderType
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import com.tonyodev.fetch2rx.RxFetch.Impl.setDefaultRxInstanceConfiguration
import okhttp3.OkHttpClient


class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
//            Timber.plant(DebugTree())
        }
        val fetchConfiguration = FetchConfiguration.Builder(this)
            .enableRetryOnNetworkGain(true)
            .setDownloadConcurrentLimit(3)
            .setHttpDownloader(HttpUrlConnectionDownloader(FileDownloaderType.PARALLEL)) // OR
            //.setHttpDownloader(getOkHttpDownloader())
            .build()
        setDefaultInstanceConfiguration(fetchConfiguration)
        setDefaultRxInstanceConfiguration(fetchConfiguration)
    }

    private val okHttpDownloader: OkHttpDownloader
        private get() {
            val okHttpClient = OkHttpClient.Builder().build()
            return OkHttpDownloader(okHttpClient, FileDownloaderType.PARALLEL)
        }
}