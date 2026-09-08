package com.example.zyvo.ui.components

import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.zyvo.media.ZegoLiveMediaEngine

/**
 * ZegoVideoView
 *
 * Renders live video feeds via ZEGOCLOUD:
 * - If [isHost] is true: Renders the local host camera preview via [ZegoLiveMediaEngine.startPreview].
 * - If [isHost] is false: Renders the remote live broadcast stream via [ZegoLiveMediaEngine.startPlaying].
 */
@Composable
fun ZegoVideoView(
    modifier: Modifier = Modifier,
    isHost: Boolean,
    streamId: String? = null,
    isMirror: Boolean = false,
    mediaEngine: ZegoLiveMediaEngine = remember { ZegoLiveMediaEngine.getInstance() }
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                TextureView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleX = if (isMirror) -1f else 1f

                    if (isHost) {
                        mediaEngine.startPreview(this)
                    } else if (!streamId.isNullOrBlank()) {
                        mediaEngine.startPlaying(streamId, this)
                    }
                }
            },
            update = { view ->
                view.scaleX = if (isMirror) -1f else 1f
                if (!isHost && !streamId.isNullOrBlank()) {
                    mediaEngine.startPlaying(streamId, view)
                }
            },
            onRelease = { view ->
                if (isHost) {
                    mediaEngine.stopPreview()
                } else if (!streamId.isNullOrBlank()) {
                    mediaEngine.stopPlaying(streamId)
                }
            }
        )
    }
}
