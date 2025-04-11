package com.example.vyorius

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.util.VLCVideoLayout
import androidx.compose.ui.graphics.Color

@Composable
fun RTSPStreamUI(
    onSurfaceReady: (VLCVideoLayout) -> Unit,
    onPlay: (String) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onEnterPip: () -> Unit,
    onViewRecordings: () -> Unit,
    isRecording: Boolean
){
    val rtspUrl = remember { mutableStateOf("rtsp://") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Video Surface
        AndroidView(
            factory = {
                VLCVideoLayout(it).apply {
                    visibility = android.view.View.VISIBLE // 👈 Ensure it's visible
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    post{ onSurfaceReady(this) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp) // fixed height so buttons are visible
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Editable RTSP URL
        OutlinedTextField(
            value = rtspUrl.value,
            onValueChange = { rtspUrl.value = it },
            label = { Text("RTSP URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Button(
            onClick = { onPlay(rtspUrl.value) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play Stream")
        }

        if (isRecording) {
            Text("● Recording", color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(
            onClick = onStartRecord,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRecording
        ) {
            Text("Start Recording")
        }

        Button(
            onClick = onStopRecord,
            modifier = Modifier.fillMaxWidth(),
            enabled = isRecording
        ) {
            Text("Stop Recording")
        }

        Button(
            onClick = onViewRecordings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Recordings")
        }

        Button(
            onClick = onEnterPip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter PiP Mode")
        }
    }
}
