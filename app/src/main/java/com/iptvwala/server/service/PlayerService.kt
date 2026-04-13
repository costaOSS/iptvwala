package com.iptvwala.server.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.iptvwala.IPTVwalaApp
import com.iptvwala.R
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.model.PlaybackState
import com.iptvwala.domain.repository.SettingsRepository
import com.iptvwala.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlayerService : MediaSessionService() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()
    
    private var retryCount = 0
    private val maxRetries = 3
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeMediaSession()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    private fun initializePlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                Media3LoadControl.getMinBufferMs(),
                Media3LoadControl.getMaxBufferMs(),
                1500,
                2000
            )
            .build()
        
        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
            }
    }
    
    private fun initializeMediaSession() {
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(mediaSessionCallback)
            .build()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "IPTVwala::PlayerWakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L)
        }
    }
    
    fun playChannel(channel: Channel, position: Long = 0) {
        _currentChannel.value = channel
        retryCount = 0
        
        player?.apply {
            stop()
            clearMediaItems()
            
            val mediaItem = MediaItem.Builder()
                .setUri(channel.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(channel.name)
                        .setArtworkUri(channel.logo?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()
            
            setMediaItem(mediaItem)
            seekTo(position)
            prepare()
            playWhenReady = true
        }
        
        updateNotification()
    }
    
    fun playUrl(url: String, title: String, logo: String? = null) {
        player?.apply {
            stop()
            clearMediaItems()
            
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtworkUri(logo?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()
            
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }
    
    fun seekTo(position: Long) {
        player?.seekTo(position)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playbackState.update { it.copy(isBuffering = true, error = null) }
                }
                Player.STATE_READY -> {
                    _playbackState.update { 
                        it.copy(
                            isBuffering = false, 
                            isPlaying = player?.isPlaying == true,
                            duration = player?.duration ?: 0
                        ) 
                    }
                    retryCount = 0
                }
                Player.STATE_ENDED -> {
                    _playbackState.update { it.copy(isPlaying = false) }
                }
                Player.STATE_IDLE -> {
                    _playbackState.update { it.copy(isPlaying = false, isBuffering = false) }
                }
            }
            updateNotification()
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            updateNotification()
        }
        
        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update { it.copy(error = error.message, isBuffering = false) }
            handlePlaybackError()
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _playbackState.update { it.copy(position = newPosition.positionMs) }
        }
    }
    
    private fun handlePlaybackError() {
        if (retryCount < maxRetries) {
            retryCount++
            val delay = (2 * retryCount * 1000).toLong()
            serviceScope.launch {
                delay(delay)
                player?.prepare()
                player?.play()
            }
        } else {
            retryCount = 0
        }
    }
    
    private fun createNotification(): Notification {
        val channel = _currentChannel.value
        val isPlaying = player?.isPlaying == true
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(channel?.name ?: getString(R.string.app_name))
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setSmallIcon(R.drawable.ic_channels)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player?.release()
            release()
            player = null
        }
        wakeLock?.release()
        serviceScope.cancel()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return binder
    }
    
    private val binder = PlayerBinder()
    
    inner class PlayerBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }
    
    private val mediaSessionCallback = object : MediaSession.Callback {}
    
    companion object {
        const val CHANNEL_ID = "player_channel"
        const val NOTIFICATION_ID = 1001
    }
}

@UnstableApi
object Media3LoadControl {
    fun getMinBufferMs(): Int = 15000
    fun getMaxBufferMs(): Int = 50000
}
