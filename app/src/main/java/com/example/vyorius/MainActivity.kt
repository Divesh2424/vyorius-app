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
import androidx.compose.runtime.mutableStateOf
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    val isRecording = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arrayListOf("--no-drop-late-frames", "--no-skip-frames")
        libVLC = LibVLC(this, args)
        mediaPlayer = MediaPlayer(libVLC)

        setContent {
            RTSPStreamUI(
                onSurfaceReady = { vlcVideoLayout ->
                    // Attach views inside a post block to avoid layout issues
                    vlcVideoLayout.post {
                        mediaPlayer?.attachViews(vlcVideoLayout, null, false, false)
                    }
                },
                onPlay = { url ->
                    val media = Media(libVLC, Uri.parse(url))
                    mediaPlayer?.media = media
                    media.release()

                    mediaPlayer?.setEventListener { event ->
                        when (event.type) {
                            MediaPlayer.Event.Playing -> {
                                runOnUiThread {
                                    Toast.makeText(this, "Stream started!", Toast.LENGTH_SHORT).show()
                                }
                            }

                            MediaPlayer.Event.EncounteredError -> {
                                runOnUiThread {
                                    Toast.makeText(this, "Error playing stream", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    mediaPlayer?.play()
                },
                onStartRecord = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val filePath = File(filesDir, "stream_$timestamp.mp4").absolutePath
                    mediaPlayer?.record(filePath, true)
                    isRecording.value = true
                    Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                },
                onStopRecord = {
                    mediaPlayer?.record(null, false)
                    isRecording.value = false
                    Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
                },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    } else {
                        Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show()
                    }
                },
                        onViewRecordings = {
                    val recordings = filesDir.listFiles()
                        ?.filter { it.name.endsWith(".mp4") }
                        ?.sortedByDescending { it.lastModified() }
                        ?: emptyList()

                    val message = if (recordings.isNotEmpty()) {
                        recordings.joinToString("\n\n") {
                            val sizeInMB = it.length() / (1024 * 1024.0)
                            val lastModified = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", it.lastModified())
                            "${it.name}\nSize: %.2f MB\nLast Modified: $lastModified".format(sizeInMB)
                        }
                    } else {
                        "No recordings found."
                    }

                    AlertDialog.Builder(this)
                        .setTitle("Saved Recordings")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                },
                isRecording = isRecording.value
            )
        }
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
        // ❌ DO NOT stop or detach VLC here
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // Optionally hide controls, but make sure video stays attached
            // Ensure VLCVideoLayout is still attached properly
            mediaPlayer?.setVideoTrackEnabled(true)
        } else {
            // Restore UI after PiP
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        libVLC?.release()
    }
}
