package com.ariful.videotimelineview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.nightcoder.mobile.timeline.TimelineView
import android.content.Intent
import android.util.Log
import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.animation.LinearInterpolator
import android.widget.RadioGroup
import android.widget.TextView

import androidx.activity.result.ActivityResultCallback

import androidx.activity.result.contract.ActivityResultContracts

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import java.lang.Exception


class MainActivity : AppCompatActivity(), TimelineView.Callback {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val REQUEST_TAKE_GALLERY_VIDEO = 115
    lateinit var timeline: TimelineView
    lateinit var player: SimpleExoPlayer
    lateinit var playerView: PlayerView
    lateinit var radioGroup: RadioGroup

    lateinit var leftTime: TextView
    lateinit var rightTime: TextView

    var pickerLauncher = registerForActivityResult(
        StartActivityForResult(),
        ActivityResultCallback { result ->
            if (result.data?.data != null && result.resultCode == RESULT_OK) {
                initPlayer(result.data!!.data!!)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        timeline = findViewById(R.id.timeLineView)
        playerView = findViewById(R.id.playerView)
        leftTime = findViewById(R.id.leftProgress)
        rightTime = findViewById(R.id.rightProgress)
        radioGroup = findViewById(R.id.modeGroup)
        radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.cutMode -> {
                    timeline.setCutMode(TimelineView.CutMode.CUT)
                }
                else -> {
                    timeline.setCutMode(TimelineView.CutMode.TRIM)
                }
            }
        }
        player = SimpleExoPlayer.Builder(this).build()
        findViewById<View>(R.id.select_video).setOnClickListener {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            pickerLauncher.launch(intent)

        }
        initProgress()
    }

    private lateinit var mHandler: Handler
    private fun initProgress() {
        player.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                mHandler.removeCallbacksAndMessages(null)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    mHandler.sendEmptyMessageDelayed(0, 16)
                } else {
                    mHandler.removeCallbacksAndMessages(null)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (state == ExoPlayer.STATE_READY) {
                    if (playerView.scaleX == 0f) {
                        playerView.animate().apply {
                            interpolator = LinearInterpolator()
                            duration = 200
                            scaleX(1f)
                            scaleY(1f)
                            start()
                        }

                    }

                    timeline.setTotalDuration(player.duration)
                    timeline.updateProgress(0.2f, 0.9f)
                    timeline.rightPosition
                    leftTime.text = 0.toLong().toPreciseReadableTime()
                    rightTime.text = player.duration.toPreciseReadableTime()
                    /*if (endTime == 0L) {
                        endTime = player.duration
                        updateLeftRightProgress()
                    }*/
                }
            }
        })
        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                try {
                    val progress = (player.currentPosition.toFloat() / player.duration.toFloat())
                    timeline.setCurrentProgressValue(progress)
                    // updateProgressDependentComponent(progress)
                } catch (ex: Exception) {
                }
                if (player.isPlaying)
                    mHandler.sendEmptyMessageDelayed(0, 16)
            }
        }
        mHandler.sendEmptyMessage(0)
    }

    private fun initPlayer(uri: Uri) {
        playerView.player = player
        playerView.player?.setMediaItem(MediaItem.fromUri(uri))
        playerView.player?.prepare()
        playerView.player?.playWhenReady = true
        timeline.setVideoUri(uri)
        timeline.callback = this

    }

    override fun onSeek(position: Float, seekMillis: Long) {

    }

    override fun onSeekStart(position: Float, seekMillis: Long) {
    }

    override fun onStopSeek(position: Float, seekMillis: Long) {
        try {
            player.seekTo(seekMillis)
        } catch (ex: Exception) {
        }

    }

    override fun onLeftProgress(leftPos: Float, seekMillis: Long) {
        try {
            Log.d(TAG, "onLeftProgress: $leftPos")
            leftTime.text = seekMillis.toPreciseReadableTime()
        } catch (ex: Exception) {
            Log.e(TAG, "onLeftProgress: ", ex)
        }
    }

    override fun onRightProgress(rightPos: Float, seekMillis: Long) {
        try {
            rightTime.text = seekMillis.toPreciseReadableTime()
        } catch (ex: Exception) {
        }
    }

    private fun Long?.toPreciseReadableTime(): String {
        if (this == null) return "--"
        if (this < 0) return "00:00:00"
        val sec = (this / 1000) % 60
        val min = (this / (1000 * 60) % 60)
        val hr = (this / (1000 * 60 * 60) % 24)
        val ms = this % 1000
        return String.format("%02d:%02d:%02d.%02d", hr, min, sec, ms)
    }

    /*private fun updateProgressDependentComponent(progress: Float) {
        binding.playerStart.text = player.currentPosition.toReadableTime()
        binding.playerEnd.text = player.duration.toReadableTime()
        binding.timeLineView.setCurrentProgressValue(progress)
    }*/


}