package com.example.vocalremover

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnSelectAudio: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSelectAudio = findViewById(R.id.btnSelectAudio)
        progressBar = findViewById(R.id.progressBar)

        btnSelectAudio.setOnClickListener {
            selectAudio()
        }
    }

    private fun selectAudio() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            uri?.let { removeVocal(it) }
        }
    }

    private fun removeVocal(uri: Uri) {
        progressBar.visibility = android.view.View.VISIBLE
        btnSelectAudio.isEnabled = false

        Thread {
            try {
                val sourcePath = getRealPathFromURI(uri) ?: throw Exception("Не удалось получить путь")
                val outputPath = File(applicationContext.externalCacheDir, "instrumental.mp3").absolutePath

                // Команда для удаления вокала через ffmpeg (подавление центрального канала)
                val cmd = "ffmpeg -i \"$sourcePath\" -af \"pan=stereo|c0=c0-c1|c1=c1-c0\" -y \"$outputPath\""

                val proc = Runtime.getRuntime().exec(cmd)
                val exitCode = proc.waitFor()

                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                    if (exitCode == 0) {
                        Toast.makeText(this, "✅ Готово! Сохранено: $outputPath", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "❌ Ошибка ffmpeg: $exitCode", Toast.LENGTH_LONG).show()
                    }
                    btnSelectAudio.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = android.view.View.GONE
                    btnSelectAudio.isEnabled = true
                }
            }
        }.start()
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(android.provider.MediaStore.Audio.AudioColumns.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return uri.path
    }
}
