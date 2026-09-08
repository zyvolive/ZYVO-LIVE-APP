package com.example.zyvo.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.zyvo.rtc.LocalMediaManager

/**
 * CameraPreviewView
 *
 * Embeds the real Android CameraX PreviewView into Jetpack Compose.
 * Handles lifecycle events (ON_START, ON_STOP, ON_RESUME) and
 * connects to LocalMediaManager's surface provider.
 */
@Composable
fun CameraPreviewView(
    mediaManager: LocalMediaManager,
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

    // Lifecycle observer to handle app foreground/background
    DisposableEffect(lifecycleOwner, mediaManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    previewViewInstance?.let { view ->
                        mediaManager.attachPreviewSurface(view.surfaceProvider, lifecycleOwner)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // CameraX handles lifecycle pause automatically when bound to lifecycleOwner
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mediaManager.detachPreviewSurface()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mediaManager.detachPreviewSurface()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.scaleType = scaleType
                    previewViewInstance = this
                    mediaManager.attachPreviewSurface(this.surfaceProvider, lifecycleOwner)
                }
            },
            update = { view ->
                if (previewViewInstance != view) {
                    previewViewInstance = view
                    mediaManager.attachPreviewSurface(view.surfaceProvider, lifecycleOwner)
                }
            }
        )
    }
}
