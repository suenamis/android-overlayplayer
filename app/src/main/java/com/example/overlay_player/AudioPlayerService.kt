package com.example.overlay_player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import java.io.IOException
import java.util.Locale

class AudioPlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "overlay_player_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.example.overlay_player.PLAY"
        const val ACTION_PAUSE = "com.example.overlay_player.PAUSE"
        const val ACTION_STOP = "com.example.overlay_player.STOP"
        const val ACTION_FORWARD_10 = "com.example.overlay_player.FORWARD_10"
        const val ACTION_BACKWARD_10 = "com.example.overlay_player.BACKWARD_10"
        const val ACTION_FORWARD_30 = "com.example.overlay_player.FORWARD_30"
        const val ACTION_BACKWARD_30 = "com.example.overlay_player.BACKWARD_30"
        const val ACTION_SHOW_OVERLAY = "com.example.overlay_player.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.overlay_player.HIDE_OVERLAY"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_TITLE = "extra_title"
        const val PREFS_NAME = "overlay_player_prefs"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentUri: Uri? = null
    private var currentTitle: String = "Unbekannt"
    private var isPlaying = false
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    // Overlay views
    private var tvTitle: TextView? = null
    private var tvCurrentTime: TextView? = null
    private var tvTotalTime: TextView? = null
    private var seekBar: SeekBar? = null
    private var btnPlayPause: ImageButton? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_URI)
                }
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Unbekannt"
                if (uri != null) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    playAudio(uri, title)
                    showOverlay()
                } else if (mediaPlayer != null) {
                    resumePlayback()
                }
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_STOP -> {
                savePosition()
                stopPlayback()
                hideOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_FORWARD_10 -> seekRelative(10_000)
            ACTION_BACKWARD_10 -> seekRelative(-10_000)
            ACTION_FORWARD_30 -> seekRelative(30_000)
            ACTION_BACKWARD_30 -> seekRelative(-30_000)
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
        }
        return START_STICKY
    }

    private fun playAudio(uri: Uri, title: String) {
        if (currentUri == uri && mediaPlayer != null) {
            resumePlayback()
            return
        }

        savePosition()
        stopPlayback()

        currentUri = uri
        currentTitle = title

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, uri)
                prepare()
                val savedPosition = getSavedPosition(uri.toString())
                if (savedPosition > 0) {
                    seekTo(savedPosition)
                }
                start()
                this@AudioPlayerService.isPlaying = true
                setOnCompletionListener {
                    clearSavedPosition(uri.toString())
                    this@AudioPlayerService.isPlaying = false
                    updateOverlayPlayState()
                    updateNotification()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
        }
        updateOverlayPlayState()
        updateOverlayInfo()
        startProgressUpdater()
        updateNotification()
    }

    private fun resumePlayback() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                isPlaying = true
                updateOverlayPlayState()
                startProgressUpdater()
                updateNotification()
            }
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                savePosition()
                updateOverlayPlayState()
                stopProgressUpdater()
                updateNotification()
            }
        }
    }

    private fun stopPlayback() {
        stopProgressUpdater()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    private fun seekRelative(ms: Int) {
        mediaPlayer?.let {
            val newPos = (it.currentPosition + ms).coerceIn(0, it.duration)
            it.seekTo(newPos)
            updateOverlayProgress()
            savePosition()
        }
    }

    // --- Position persistence ---

    private fun getSavedPosition(uriString: String): Int {
        return prefs.getInt("pos_${uriString.hashCode()}", 0)
    }

    private fun savePosition() {
        val uri = currentUri ?: return
        val pos = mediaPlayer?.currentPosition ?: return
        prefs.edit { putInt("pos_${uri.toString().hashCode()}", pos) }
    }

    private fun clearSavedPosition(uriString: String) {
        prefs.edit { remove("pos_${uriString.hashCode()}") }
    }

    // --- Overlay ---

    @SuppressLint("InflateParams")
    private fun showOverlay() {
        if (overlayView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_player, null)

        setupOverlayViews()
        setupOverlayDrag(params)

        windowManager?.addView(overlayView, params)
        updateOverlayInfo()
        updateOverlayPlayState()
        updateOverlayProgress()
    }

    private fun setupOverlayViews() {
        val view = overlayView ?: return

        tvTitle = view.findViewById(R.id.tv_title)
        tvCurrentTime = view.findViewById(R.id.tv_current_time)
        tvTotalTime = view.findViewById(R.id.tv_total_time)
        seekBar = view.findViewById(R.id.seek_bar)
        btnPlayPause = view.findViewById(R.id.btn_play_pause)

        val btnBack30 = view.findViewById<ImageButton>(R.id.btn_back_30)
        val btnBack10 = view.findViewById<ImageButton>(R.id.btn_back_10)
        val btnForward10 = view.findViewById<ImageButton>(R.id.btn_forward_10)
        val btnForward30 = view.findViewById<ImageButton>(R.id.btn_forward_30)
        val btnClose = view.findViewById<ImageButton>(R.id.btn_close)

        btnPlayPause?.setOnClickListener {
            if (isPlaying) pausePlayback() else resumePlayback()
        }
        btnBack30?.setOnClickListener { seekRelative(-30_000) }
        btnBack10?.setOnClickListener { seekRelative(-10_000) }
        btnForward10?.setOnClickListener { seekRelative(10_000) }
        btnForward30?.setOnClickListener { seekRelative(30_000) }
        btnClose?.setOnClickListener {
            savePosition()
            hideOverlay()
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    tvCurrentTime?.text = formatTime(progress)
                    savePosition()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayDrag(params: WindowManager.LayoutParams) {
        val dragHandle = overlayView?.findViewById<View>(R.id.drag_handle) ?: return
        var initialY = 0
        var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.y = initialY - (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
        }
        overlayView = null
        tvTitle = null
        tvCurrentTime = null
        tvTotalTime = null
        seekBar = null
        btnPlayPause = null
    }

    private fun updateOverlayInfo() {
        tvTitle?.text = currentTitle
        mediaPlayer?.let {
            tvTotalTime?.text = formatTime(it.duration)
            seekBar?.max = it.duration
        }
    }

    private fun updateOverlayPlayState() {
        btnPlayPause?.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateOverlayProgress() {
        mediaPlayer?.let {
            tvCurrentTime?.text = formatTime(it.currentPosition)
            seekBar?.progress = it.currentPosition
        }
    }

    // --- Progress updater ---

    private fun startProgressUpdater() {
        stopProgressUpdater()
        progressRunnable = object : Runnable {
            override fun run() {
                updateOverlayProgress()
                handler.postDelayed(this, 500)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressUpdater() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    // --- Notification ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio Player Steuerung"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, AudioPlayerService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val pendingPlayPause = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioPlayerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Player")
            .setContentText(if (isPlaying) "Spielt: $currentTitle" else "Pausiert: $currentTitle")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingOpen)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                pendingPlayPause
            )
            .addAction(R.drawable.ic_stop, "Stop", pendingStop)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    // --- Helpers ---

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        savePosition()
        stopPlayback()
        hideOverlay()
        super.onDestroy()
    }
}
