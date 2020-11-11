package com.example.fetchdownloadrx.DownloadList

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.fetchdownloadrx.R
import com.example.fetchdownloadrx.Utils
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import java.io.File

class FileAdapter(private val actionListener: ActionListener) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    @NonNull
    private val downloads: MutableList<DownloadData> = ArrayList()

//    @NonNull
//    private lateinit var actionListener: ActionListener
//
//    fun FileAdapter(actionListener: ActionListener) {
//        this.actionListener = actionListener
//    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.download_item,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.actionButton.setOnClickListener(null)
        holder.actionButton.isEnabled = true

        val downloadData = downloads[position]
        var url = ""
        if (downloadData.download != null) {
            url = downloadData.download!!.url
        }
        val uri: Uri = Uri.parse(url)
        val status: Status = downloadData.download!!.status
        val context: Context = holder.itemView.context

        holder.titleTextView.text = uri.lastPathSegment
        holder.statusTextView.text = getStatusString(status)

        var progress = downloadData.download!!.progress
        if (progress == -1) { // Download progress is undermined at the moment.
            progress = 0
        }
        holder.progressBar.progress = progress
        holder.progressTextView.text = context.getString(R.string.percent_progress, progress)

        if (downloadData.eta == -1L) {
            holder.timeRemainingTextView.text = ""
        } else {
            holder.timeRemainingTextView.text =
                Utils.getETAString(context, downloadData.eta)
        }

        if (downloadData.downloadedBytesPerSecond == 0L) {
            holder.downloadedBytesPerSecondTextView.text = ""
        } else {
            holder.downloadedBytesPerSecondTextView.text =
                Utils.getDownloadSpeedString(context, downloadData.downloadedBytesPerSecond)
        }

        when (status) {
            Status.COMPLETED -> {
                holder.actionButton.setText(R.string.view)
                holder.actionButton.setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Toast.makeText(
                            context,
                            "Downloaded Path:" + downloadData.download!!.file,
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    val file = File(downloadData.download!!.file)
                    val uri1: Uri = Uri.fromFile(file)
                    val share = Intent(Intent.ACTION_VIEW)
                    share.setDataAndType(uri1, Utils.getMimeType(context, uri1))
                    context.startActivity(share)
                }
            }
            Status.FAILED -> {
                holder.actionButton.setText(R.string.retry)
                holder.actionButton.setOnClickListener {
                    holder.actionButton.isEnabled = false
                    actionListener.onRetryDownload(downloadData.download!!.id)
                }
            }
            Status.PAUSED -> {
                holder.actionButton.setText(R.string.resume)
                holder.actionButton.setOnClickListener {
                    holder.actionButton.isEnabled = false
                    actionListener.onResumeDownload(downloadData.download!!.id)
                }
            }
            Status.DOWNLOADING, Status.QUEUED -> {
                holder.actionButton.setText(R.string.pause)
                holder.actionButton.setOnClickListener {
                    holder.actionButton.isEnabled = false
                    actionListener.onPauseDownload(downloadData.download!!.id)
                }
            }
            Status.ADDED -> {
                holder.actionButton.setText(R.string.download)
                holder.actionButton.setOnClickListener {
                    holder.actionButton.isEnabled = false
                    actionListener.onResumeDownload(downloadData.download!!.id)
                }
            }
            else -> {
            }
        }

        //Set delete action

        //Set delete action
        holder.itemView.setOnLongClickListener { v ->
            val uri12: Uri = Uri.parse(downloadData.download!!.url)
            AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.delete_title, uri12.lastPathSegment))
                .setPositiveButton(R.string.delete) { dialog, which ->
                    actionListener.onRemoveDownload(
                        downloadData.download!!.id
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            true
        }
    }

    fun addDownload(download: Download) {
        var found = false
        var data: DownloadData? = null
        var dataPosition = -1
        for (i in downloads.indices) {
            val downloadData = downloads[i]
            if (downloadData.id == download.id) {
                data = downloadData
                dataPosition = i
                found = true
                break
            }
        }
        if (!found) {
            val downloadData = DownloadData()
            downloadData.id = download.id
            downloadData.download = download
            downloads.add(downloadData)
            notifyItemInserted(downloads.size - 1)
        } else {
            data!!.download = download
            notifyItemChanged(dataPosition)
        }
    }

    override fun getItemCount(): Int {
        return downloads.size
    }

    fun update(download: Download, eta: Long, downloadedBytesPerSecond: Long) {
        for (position in downloads.indices) {
            val downloadData = downloads[position]
            if (downloadData.id == download.id) {
                when (download.status) {
                    Status.REMOVED, Status.DELETED -> {
                        downloads.removeAt(position)
                        notifyItemRemoved(position)
                    }
                    else -> {
                        downloadData.download = download
                        downloadData.eta = eta
                        downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond
                        notifyItemChanged(position)
                    }
                }
                return
            }
        }
    }

    private fun getStatusString(status: Status): String? {
        return when (status) {
            Status.COMPLETED -> "Done"
            Status.DOWNLOADING -> "Downloading"
            Status.FAILED -> "Error"
            Status.PAUSED -> "Paused"
            Status.QUEUED -> "Waiting in Queue"
            Status.REMOVED -> "Removed"
            Status.NONE -> "Not Queued"
            else -> "Unknown"
        }
    }

    companion object{
        fun addDownload(download: Download){
            addDownload(download)
        }
    }


    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById<TextView>(R.id.titleTextView)
        val statusTextView: TextView = itemView.findViewById<TextView>(R.id.status_TextView)
        val progressBar: ProgressBar = itemView.findViewById<ProgressBar>(R.id.progressBar)
        val progressTextView: TextView = itemView.findViewById<TextView>(R.id.progress_TextView)
        val actionButton: Button = itemView.findViewById(R.id.actionButton)
        val timeRemainingTextView: TextView =
            itemView.findViewById<TextView>(R.id.remaining_TextView)
        val downloadedBytesPerSecondTextView: TextView =
            itemView.findViewById<TextView>(R.id.downloadSpeedTextView)
    }

    class DownloadData {
        var id = 0

        @Nullable
        var download: Download? = null
        var eta: Long = -1
        var downloadedBytesPerSecond: Long = 0
        override fun hashCode(): Int {
            return id
        }

        override fun toString(): String {
            return if (download == null) {
                ""
            } else download.toString()
        }

        override fun equals(obj: Any?): Boolean {
            return obj === this || obj is DownloadData && obj.id == id
        }
    }
}