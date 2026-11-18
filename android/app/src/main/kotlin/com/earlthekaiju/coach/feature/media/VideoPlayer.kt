package com.earlthekaiju.coach.feature.media

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.earlthekaiju.coach.R
import com.earlthekaiju.coach.data.db.MediaClipEntity
import kotlinx.coroutines.delay
import java.io.File

/**
 * The library's video player: one ExoPlayer per open video screen, with the controls a coach
 * needs to study a move: loop, slow motion (½× and ¼×), mirror (show the other side), and
 * sound off (so the class music keeps playing).
 *
 * A clip plays as its own short item (ExoPlayer clipping), so the bar, the time and the loop all
 * cover just the clip. [absolutePositionMs] converts back to a time in the whole file.
 */
@Stable
class KaijuPlayerState(val player: ExoPlayer) {
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var speed by mutableFloatStateOf(1f)
    var loop by mutableStateOf(false)
    var mirror by mutableStateOf(false)
    var sound by mutableStateOf(true)

    /** Where the loaded item starts inside the file: the clip's start, or 0 for the whole video. */
    var offsetMs by mutableLongStateOf(0L)
        private set
    private var loadedKey: String? = null

    val absolutePositionMs: Long get() = offsetMs + positionMs

    fun load(file: File, clip: MediaClipEntity?) {
        val key = "${file.path}#${clip?.id}:${clip?.startMs}-${clip?.endMs}"
        if (key == loadedKey) return
        loadedKey = key
        val builder = MediaItem.Builder().setUri(Uri.fromFile(file))
        if (clip != null) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs)
                    .build(),
            )
        }
        player.setMediaItem(builder.build())
        player.prepare()
        offsetMs = clip?.startMs ?: 0L
        positionMs = 0L
        // A clip is for studying a move: it loops by default and starts right away.
        loop = clip != null
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (clip != null) player.play()
    }

    fun toggle() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        positionMs = ms
    }

    fun cycleSpeed() {
        speed = when (speed) {
            1f -> 0.5f
            0.5f -> 0.25f
            else -> 1f
        }
        player.setPlaybackSpeed(speed)
    }

    fun toggleLoop() {
        loop = !loop
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleMirror() {
        mirror = !mirror
    }

    /** Sound off also gives up audio focus, so Spotify (or whatever's playing) isn't paused. */
    fun toggleSound() {
        sound = !sound
        applySound()
    }

    fun applySound() {
        player.setAudioAttributes(MOVIE, sound)
        player.volume = if (sound) 1f else 0f
    }

    private companion object {
        val MOVIE: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }
}

@Composable
fun rememberKaijuPlayer(): KaijuPlayerState {
    val context = LocalContext.current
    val state = remember { KaijuPlayerState(ExoPlayer.Builder(context).build()).also { it.applySound() } }
    DisposableEffect(state) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val d = state.player.duration
                if (d != C.TIME_UNSET && d > 0) state.durationMs = d
            }
        }
        state.player.addListener(listener)
        onDispose {
            state.player.removeListener(listener)
            state.player.release()
        }
    }
    // The bar and the time follow the player five times a second.
    LaunchedEffect(state) {
        while (true) {
            state.positionMs = state.player.currentPosition.coerceAtLeast(0L)
            val d = state.player.duration
            if (d != C.TIME_UNSET && d > 0) state.durationMs = d
            delay(200)
        }
    }
    // Leaving the app pauses the video.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) state.player.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return state
}

/** The video picture. A TextureView-backed PlayerView (see kaiju_player_view.xml), mirrored on demand. */
@Composable
fun KaijuPlayerSurface(state: KaijuPlayerState, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.kaiju_player_view, null, false) as PlayerView).also { it.player = state.player }
        },
        modifier = modifier.graphicsLayer { scaleX = if (state.mirror) -1f else 1f },
        onRelease = { it.player = null },
    )
}
