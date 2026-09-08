package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.zyvo.rtc.PeerConnectionFactoryProvider
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * WebRtcVideoView
 *
 * Renders an org.webrtc.VideoTrack (either local camera track or incoming remote track)
 * using WebRTC's hardware-accelerated SurfaceViewRenderer and EglBase context.
 */
@Composable
fun WebRtcVideoView(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    isMirror: Boolean = false,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL
) {
    val eglBaseContext = PeerConnectionFactoryProvider.eglBase?.eglBaseContext

    if (eglBaseContext == null || videoTrack == null) {
        Box(modifier = modifier.background(Color.Black))
        return
    }

    var rendererInstance by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(videoTrack) {
        val currentRenderer = rendererInstance
        currentRenderer?.let { renderer ->
            videoTrack.addSink(renderer)
        }
        onDispose {
            currentRenderer?.let { renderer ->
                videoTrack.removeSink(renderer)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglBaseContext, null)
                setScalingType(scalingType)
                setEnableHardwareScaler(true)
                setMirror(isMirror)
                videoTrack.addSink(this)
                rendererInstance = this
            }
        },
        update = { renderer ->
            renderer.setMirror(isMirror)
            renderer.setScalingType(scalingType)
        },
        onRelease = { renderer ->
            videoTrack.removeSink(renderer)
            renderer.release()
            rendererInstance = null
        },
        modifier = modifier.fillMaxSize()
    )
}
