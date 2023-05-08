/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.navigation.component

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.preference.dataStore
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.debug

const val CONTROLS_TIMEOUT_MS = 7500L

class VideoPlayerComponent(
    url: String,
    applicationContext: Context,
    mediaOkHttpClient: OkHttpClient,
    componentContext: ComponentContext,
    private val controlsTimeoutMs: Long = CONTROLS_TIMEOUT_MS
) : ComponentContext by componentContext, Player.Listener, Lifecycle.Callbacks {
    private val instance = instanceKeeper.getOrCreate {
        Instance(applicationContext, mediaOkHttpClient)
    }

    private val lifecycleScope = LifecycleScope()
    private val dataStore = applicationContext.dataStore


    // UI state
    private var hideControlsTimeoutJob: Job? = null
    private var _showControls by mutableStateOf(false)
    private var _isMuted by mutableStateOf(true)
    private var _showRemainingInsteadOfTotalTime by mutableStateOf(false)
    var showControls
        get() = _showControls
        set(v) {
            _showControls = v
            if (v) {
                hideControlsTimeoutJob?.cancel()
                hideControlsTimeoutJob = lifecycleScope.launch {
                    delay(controlsTimeoutMs)
                    _showControls = false
                    hideControlsTimeoutJob = null
                }
            } else {
                hideControlsTimeoutJob?.cancel()
                hideControlsTimeoutJob = null
            }
        }

    var isMuted
        get() = _isMuted
        set(v) {
            with(player) {
                if (v) {
                    volume = 0f
                    setAudioAttributes(AudioAttributes.DEFAULT, false)
                } else {
                    volume = 1f
                    setAudioAttributes(AudioAttributes.DEFAULT, true)
                }
            }
            _isMuted = v
        }

    var showRemainingInsteadOfTotalTime
        get() = _showRemainingInsteadOfTotalTime
        set(v) {
            _showRemainingInsteadOfTotalTime = v
            lifecycleScope.launch {
                dataStore.updatePreferences {
                    showRemainingTimeMedia = v
                }
            }
        }

    fun resetControlsTimeout() {
        // Basically restart job
        showControls = true
    }

    // Player state
    val player by instance::player

    var timestamp by mutableStateOf(getCurrentTimestamp())
        private set

    var isPlaying by mutableStateOf(player.isPlaying)
        private set
    var contentDurationMs by mutableStateOf(player.contentDuration.coerceAtLeast(0))
        private set

    @get:Player.State
    var playbackState by mutableStateOf(player.playbackState)
        private set

    private var _playWhenReady by mutableStateOf(player.playWhenReady)

    var playWhenReady: Boolean
        get() = _playWhenReady
        set(v) {
            // I know about snapshotFlow, but let's do it without coroutines
            player.playWhenReady = v
            _playWhenReady = v
        }

    init {
        lifecycle.subscribe(this)
        player.addListener(this)
        dataStore.data.take(1)
            .onEach {
                isMuted = it.muteSoundOnMedia
                showRemainingInsteadOfTotalTime = it.showRemainingTimeMedia
            }
            .launchIn(lifecycleScope)
        setUrl(url)
    }

    override fun onResume() {
        debug {
            Log.d(TAG, "Restoring state: ${instance.playbackSavedState}")
        }
        if (!player.availableCommands.contains(Player.COMMAND_PLAY_PAUSE)) Log.w(
            TAG,
            "playWhenReady modified while command is not available"
        )
        when (instance.playbackSavedState) {
            PlaybackSavedState.PAUSED -> {
                playWhenReady = true
            }

            PlaybackSavedState.UNCHANGED -> {}
            PlaybackSavedState.EMPTY -> {
                dataStore.getPreferencesFlow { it.autoplayOnPostOpen }
                    .take(1)
                    .onEach {
                        if (lifecycle.state == Lifecycle.State.RESUMED)
                            playWhenReady = it
                    }
                    .launchIn(lifecycleScope)
            }
        }

    }

    override fun onPause() {
        if (!instance.destroyed) {
            instance.playbackSavedState =
                if (playWhenReady) PlaybackSavedState.PAUSED else PlaybackSavedState.UNCHANGED
            playWhenReady = false
        }
    }

    override fun onDestroy() {
        player.removeListener(this)
    }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        if (availableCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
            debug {
                Log.d(
                    TAG,
                    "Player playWhenReady=${player.playWhenReady}, component playWhenReady=$playWhenReady"
                )
            }
            // Just in case
            if (player.playWhenReady != playWhenReady) {
                player.playWhenReady = playWhenReady
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        updateTimestamp()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateTimestamp()
        this.isPlaying = isPlaying
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateTimestamp()
        contentDurationMs = player.contentDuration.coerceAtLeast(0)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        this.playbackState = playbackState
    }

    private fun updateTimestamp() {
        timestamp = getCurrentTimestamp()
    }

    private fun getCurrentTimestamp() = Timestamp(
        System.currentTimeMillis(),
        player.currentPosition.coerceAtLeast(0),
        player.contentPosition.coerceAtLeast(0),
        player.playbackParameters.speed
    )

    data class Timestamp(
        val anchorMs: Long,
        val positionMs: Long,
        val contentPositionMs: Long,
        val speed: Float
    )

    fun setUrl(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private class Instance(
        applicationContext: Context,
        mediaOkHttpClient: OkHttpClient
    ) : InstanceBase(), Player.Listener {
        val player = ExoPlayer.Builder(applicationContext)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    OkHttpDataSource.Factory {
                        mediaOkHttpClient.newCall(it)
                    }
                )
            )
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ALL
            }

        var playbackSavedState = PlaybackSavedState.EMPTY

        var destroyed = false
            private set

        override fun onDestroy() {
            super.onDestroy()
            // https://github.com/arkivanov/Decompose/issues/383
            destroyed = true
            player.release()
        }
    }

    companion object {
        private const val TAG = "VideoPlayerComponent"
    }

    enum class PlaybackSavedState {
        PAUSED,
        UNCHANGED,
        EMPTY
    }
}