package com.example.vyorius

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun RTSPStreamUI(
    onSurfaceReady: (VLCVideoLayout) -> Unit,
    onPlay: (String) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onEnterPip: () -> Unit,
    onViewRecordings: () -> Unit,
    isRecording: Boolean,
    isInPipMode: Boolean
) {
    val rtspUrl = remember { mutableStateOf("rtsp://admin:admin@192.168.0.107:8554/live") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Video Surface
        AndroidView(
            factory = {
                VLCVideoLayout(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    onSurfaceReady(this)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isInPipMode) 240.dp else 240.dp)
        )
        if (!isInPipMode) {
            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(8.dp))

            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recording...", color = Color.Red)
                }
            }

            OutlinedTextField(
                value = rtspUrl.value,
                onValueChange = { rtspUrl.value = it },
                label = { Text("RTSP URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onPlay(rtspUrl.value) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Play Stream")
            }

            if (!isRecording) {
                Button(
                    onClick = onStartRecord,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Recording")
                }
            } else {
                Button(
                    onClick = onStopRecord,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Stop Recording", color = Color.White)
                }
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
}