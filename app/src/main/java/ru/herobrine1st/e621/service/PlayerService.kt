package ru.herobrine1st.e621.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import ru.herobrine1st.e621.R
import javax.inject.Inject


@AndroidEntryPoint
class PlayerService : Service(), Player.Listener, PlayerNotificationManager.NotificationListener {
    @Inject
    lateinit var exoPlayer: ExoPlayer

    private val binder: IBinder = LocalBinder()
    private lateinit var notificationManager: PlayerNotificationManager

    override fun onBind(intent: Intent?) = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        exoPlayer.addListener(this)
        notificationManager = PlayerNotificationManager.Builder(
            applicationContext,
            R.id.mediaPlayerNotification,
            "mediaPlayerChannel"
        )
            .setChannelNameResourceId(R.string.channel_name_media_player)
            .setNotificationListener(this)
            .build()
        notificationManager.setPlayer(exoPlayer)
    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        super.onStartCommand(intent, flags, startId)
//        return START_NOT_STICKY
//    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        exoPlayer.removeListener(this)
        notificationManager.setPlayer(null)
    }

    // EXOPLAYER LISTENER

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Log.d(TAG, "onMediaItemTransition($mediaItem, $reason)")

        if (mediaItem == null) {
            notificationManager.setPlayer(null)
        } else {
            notificationManager.setPlayer(exoPlayer)
        }
    }

    // NOTIFICATION LISTENER

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        Log.d(TAG, "onNotificationPosted($notificationId, hidden, $ongoing)")
        if (ongoing) {
            startForeground(notificationId, notification)
        } else {
            stopForeground(false)
        }
    }

    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        Log.d(TAG, "onNotificationCancelled()")
        stopSelf()
    }

    //

    inner class LocalBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }

    companion object {
        const val TAG = "PlayerService"
    }
}