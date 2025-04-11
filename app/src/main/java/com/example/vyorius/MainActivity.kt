package com.example.vyorius

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.arthenica.ffmpegkit.FFmpegKit
import kotlinx.coroutines.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.*


class MainActivity : AppCompatActivity() {
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private var currentRecordingFile: File? = null
    private var currentStreamUrl: String = ""
    private var isInPipMode = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        libVLC = LibVLC(this, arrayListOf("--no-drop-late-frames", "--no-skip-frames"))
        mediaPlayer = MediaPlayer(libVLC)

        setContent {
            var pipState by remember { mutableStateOf(isInPipMode) }

            // Sync changes from system callback
            LaunchedEffect(isInPipMode) {
                pipState = isInPipMode
            }
            RTSPStreamUI(
                onSurfaceReady = { vlcVideoLayout ->
                    mediaPlayer?.attachViews(vlcVideoLayout, null, false, false)
                },
                onPlay = { url ->
                    currentStreamUrl = url
                    val media = Media(libVLC, Uri.parse(url))
                    mediaPlayer?.media = media
                    media.release()
                    mediaPlayer?.play()
                    Toast.makeText(this, "Stream started", Toast.LENGTH_SHORT).show()
                },
                onStartRecord = {
                    if (!isRecording) startFFmpegRecording()
                },
                onStopRecord = {
                    if (isRecording) stopFFmpegRecording()
                },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    }
                },
                onViewRecordings = {
                    val recordings = filesDir.listFiles()?.filter { it.name.endsWith(".mp4") } ?: emptyList()
                    val message = if (recordings.isNotEmpty())
                        recordings.joinToString("\n") { it.name }
                    else "No recordings found."

                    AlertDialog.Builder(this)
                        .setTitle("Saved Recordings")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                },
                isRecording = isRecording,
                        isInPipMode = isInPipMode
            )
        }
    }

    private fun startFFmpegRecording() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(filesDir, "stream_$timestamp.mp4")
        currentRecordingFile = outputFile
        isRecording = true

        val command = "-i $currentStreamUrl -c copy -f mp4 ${outputFile.absolutePath}"

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val session = FFmpegKit.execute(command)
            withContext(Dispatchers.Main) {
                isRecording = false
                if (session.returnCode.isValueSuccess) {
                    Toast.makeText(this@MainActivity, "Recording saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Recording failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show()
    }

    private fun stopFFmpegRecording() {
        recordingJob?.cancel()
        isRecording = false
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        libVLC?.release()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}
