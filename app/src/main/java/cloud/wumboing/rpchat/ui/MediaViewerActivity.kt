package cloud.wumboing.rpchat.ui

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.databinding.ActivityMediaViewerBinding
import cloud.wumboing.rpchat.util.BitmapUtils
import java.io.File
import java.util.Locale

class MediaViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_NAME = "extra_name"
    }

    private lateinit var binding: ActivityMediaViewerBinding
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (isPlaying) {
                    binding.seekAudio.progress = mp.currentPosition
                    binding.txtAudioPosition.text = formatTime(mp.currentPosition)
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val path = intent.getStringExtra(EXTRA_PATH)
        val type = intent.getStringExtra(EXTRA_TYPE)
        val name = intent.getStringExtra(EXTRA_NAME) ?: ""
        binding.toolbar.title = name

        if (path == null || !File(path).exists()) {
            finish()
            return
        }

        when (type) {
            "photo" -> showPhoto(path)
            "video" -> showVideo(path)
            "audio" -> showAudio(path, name)
            else -> finish()
        }
    }

    private fun showPhoto(path: String) {
        binding.imgPhotoViewer.visibility = View.VISIBLE
        val bmp = BitmapUtils.decodeSampledFromFile(path, 1600)
        if (bmp != null) binding.imgPhotoViewer.setImageBitmap(bmp) else finish()
    }

    private fun showVideo(path: String) {
        binding.videoViewer.visibility = View.VISIBLE
        val controller = MediaController(this)
        controller.setAnchorView(binding.videoViewer)
        binding.videoViewer.setMediaController(controller)
        binding.videoViewer.setVideoURI(Uri.fromFile(File(path)))
        binding.videoViewer.setOnPreparedListener {
            binding.videoViewer.start()
        }
        binding.videoViewer.setOnErrorListener { _, _, _ -> finish(); true }
    }

    private fun showAudio(path: String, name: String) {
        binding.audioPlayerContainer.visibility = View.VISIBLE
        binding.txtAudioName.text = name

        try {
            mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(path)))
        } catch (e: Exception) {
            mediaPlayer = null
        }
        val mp = mediaPlayer
        if (mp == null) {
            finish()
            return
        }

        binding.seekAudio.max = mp.duration
        binding.txtAudioDuration.text = formatTime(mp.duration)
        binding.txtAudioPosition.text = formatTime(0)

        binding.btnAudioPlayPause.setOnClickListener {
            if (isPlaying) {
                mp.pause()
                isPlaying = false
                binding.btnAudioPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                mp.start()
                isPlaying = true
                binding.btnAudioPlayPause.setImageResource(R.drawable.ic_pause)
                handler.post(progressRunnable)
            }
        }

        binding.seekAudio.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.txtAudioPosition.text = formatTime(progress)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                seekBar?.let { mp.seekTo(it.progress) }
            }
        })

        mp.setOnCompletionListener {
            isPlaying = false
            binding.btnAudioPlayPause.setImageResource(R.drawable.ic_play)
            binding.seekAudio.progress = 0
            binding.txtAudioPosition.text = formatTime(0)
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
