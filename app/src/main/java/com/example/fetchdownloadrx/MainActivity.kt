package com.example.fetchdownloadrx

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fetchdownloadrx.DownloadList.DownloadListActivity
import com.example.fetchdownloadrx.DownloadRx.DownloadRx


class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.singleButton).setOnClickListener {
            val intent = Intent(this, SingleDownloadActivity::class.java)
            this.startActivity(intent)
        }
        findViewById<Button>(R.id.downloadListButton).setOnClickListener {
            val intent = Intent(this, DownloadListActivity::class.java)
            this.startActivity(intent)
        }

        findViewById<Button>(R.id.gameFilesButton).setOnClickListener {
            val intent = Intent(this, DownloadRx::class.java)
            this.startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            deleteDownloadedFiles();
        } else {
            Toast.makeText(this,"permission_not_enabled", Toast.LENGTH_LONG).show();
        }
    }

}