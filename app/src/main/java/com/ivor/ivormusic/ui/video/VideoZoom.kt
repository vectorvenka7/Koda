package com.ivor.ivormusic.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize

/**
 * Free pinch zoom for the fullscreen video: 1.0x (100%) to 5.0x (500%).
 *
 * Applied on top of the resize mode the PlayerView already uses (fit or
 * zoom-to-fill), as a transform on the view, so it never touches
 * keepKnownAspectRatio or the effect graph.
 */
class VideoZoomState(
    private val minScale: Float = 1f,
    private val maxScale: Float = 5f,
) {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Size of the gesture surface. A change (rotation, chat docking) drops the zoom. */
    var size: IntSize = IntSize.Zero
        set(value) {
            if (field != value && field != IntSize.Zero) reset()
            field = value
        }

    val isZoomed: Boolean get() = scale > 1.001f
    val percent: Int get() = Math.round(scale * 100f)

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        if (size.width == 0 || size.height == 0) return
        val oldScale = scale
        val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)
        val center = Offset(size.width / 2f, size.height / 2f)

        // Keep the point under the fingers fixed while the scale changes.
        val anchored = offset + (centroid - center - offset) * (1f - newScale / oldScale)
        val moved = anchored + pan

        // newScale >= 1, so both limits are >= 0 and coerceIn never sees an empty range.
        val maxX = size.width * (newScale - 1f) / 2f
        val maxY = size.height * (newScale - 1f) / 2f
        scale = newScale
        offset = if (newScale <= 1.001f) {
            Offset.Zero
        } else {
            Offset(moved.x.coerceIn(-maxX, maxX), moved.y.coerceIn(-maxY, maxY))
        }
    }
}

@Composable
fun rememberVideoZoomState(): VideoZoomState = remember { VideoZoomState() }

/** Put on the video view, after clipToBounds(), so zoomed video cannot spill over neighbours. */
fun Modifier.videoZoomLayer(state: VideoZoomState): Modifier = graphicsLayer {
    scaleX = state.scale
    scaleY = state.scale
    translationX = state.offset.x
    translationY = state.offset.y
}
