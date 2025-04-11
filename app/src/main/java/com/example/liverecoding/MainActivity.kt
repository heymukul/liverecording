package com.example.liverecoding

import android.app.PictureInPictureParams
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.util.Rational
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import androidx.core.net.toUri
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout
    private var isRecording = false;
    private var media: Media? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        libVLC = LibVLC(this,arrayListOf("--no-drop-late-frames","--no-skip-frames"))
        mediaPlayer = MediaPlayer(libVLC)

        var urlInput = findViewById<EditText>(R.id.urlInout)
        val playButton = findViewById<Button>(R.id.playButton)
        val recordButton = findViewById<Button>(R.id.recordButton)
        val pipButton = findViewById<Button>(R.id.pipButton)
        videoLayout = findViewById(R.id.videoLayout)

        mediaPlayer.attachViews(videoLayout,null,false,false)

        playButton.setOnClickListener {
            val rtspUrl = urlInput.text.toString()
            media?.release()
            media = Media(libVLC , Uri.parse(rtspUrl)).apply{
                setHWDecoderEnabled(true,false)
                addOption(":network-caching = 150")
            }
            mediaPlayer.media = media
            mediaPlayer.play()
        }
        recordButton.setOnClickListener {
            toggleRecording()
        }
        pipButton.setOnClickListener {
                enterPipMode()

        }
    }
    private fun enterPipMode() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            videoLayout.post {
                val width = videoLayout.width
                val height = videoLayout.height
                val aspectRatio = if (width > 0 && height > 0) Rational(width, height) else Rational(16, 9)

                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()

                enterPictureInPictureMode(params)
            }
        }
    }

    private fun toggleRecording() {
        val outputPath = getRecordingOutputPath()
        if (!isRecording){
            media?.addOption(":sout=#duplicate{dst=display,dst=standard{access=file,mux=mp4,dst=$outputPath}}")
            Toast.makeText(this,"Recording Started",Toast.LENGTH_SHORT).show()
            isRecording = true
        } else {
            mediaPlayer.stop()
            Toast.makeText(this,"Recording Save", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    private fun getRecordingOutputPath(): String {
        val recordingsDir = File(getExternalFilesDir(null), "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        val fileName = "recorded_${System.currentTimeMillis()}.mp4"
        return File(recordingsDir, fileName).absolutePath
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVLC.release()
    }
}