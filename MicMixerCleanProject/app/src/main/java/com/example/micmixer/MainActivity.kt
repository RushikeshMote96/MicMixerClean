package com.example.micmixer

import android.media.*
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.NoiseSuppressor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recorder: AudioRecord
    private lateinit var track: AudioTrack
    private lateinit var musicPlayer: ExoPlayer

    private var isMicOn = false
    private var micVolume = 1f
    private var reverb: EnvironmentalReverb? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private var isSessionRecording = false
    private var wavRecorder: WavRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnMic = findViewById<Button>(R.id.btnMic)
        val btnMusic = findViewById<Button>(R.id.btnMusic)
        val btnEcho = findViewById<Button>(R.id.btnEcho)
        val btnNoise = findViewById<Button>(R.id.btnNoise)
        val btnRec = findViewById<Button>(R.id.btnRec)
        val micSeek = findViewById<SeekBar>(R.id.micVolume)
        val musicSeek = findViewById<SeekBar>(R.id.musicVolume)

        setupMusic()

        btnMic.setOnClickListener { if (!isMicOn) startMic() else stopMic() }
        btnMusic.setOnClickListener { if (musicPlayer.isPlaying) musicPlayer.pause() else musicPlayer.play() }
        micSeek.setOnSeekBarChangeListener(simpleSeek { micVolume = it / 100f })
        musicSeek.setOnSeekBarChangeListener(simpleSeek { musicPlayer.volume = it / 100f })

        btnEcho.setOnClickListener { reverb?.enabled = !(reverb?.enabled ?: false) }
        btnNoise.setOnClickListener { noiseSuppressor?.enabled = !(noiseSuppressor?.enabled ?: false) }

        btnRec.setOnClickListener {
            if (!isSessionRecording) startSessionRecording() else stopSessionRecording()
        }
    }

    private fun setupMusic() {
        musicPlayer = ExoPlayer.Builder(this).build()
        val mediaItem = MediaItem.fromUri("asset:///bg.mp3")
        musicPlayer.setMediaItem(mediaItem)
        musicPlayer.prepare()
    }

    private fun startSessionRecording() {
        val dir = getExternalFilesDir(null)!!
        val file = File(dir, "session_${System.currentTimeMillis()}.wav")
        wavRecorder = WavRecorder(file, 44100)
        wavRecorder?.start()
        isSessionRecording = true
    }

    private fun stopSessionRecording() {
        isSessionRecording = false
        wavRecorder?.stop()
    }

    private fun startMic() {
        val sr = 44100
        val buf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sr,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buf)

        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(sr)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(buf)
            .build()

        recorder.startRecording()
        track.play()

        reverb = EnvironmentalReverb(0, track.audioSessionId)
        reverb?.enabled = false

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(recorder.audioSessionId)
        }

        isMicOn = true

        Thread {
            val buffer = ByteArray(buf)
            while (isMicOn) {
                val read = recorder.read(buffer, 0, buffer.size)
                track.write(buffer, 0, read)
                if (isSessionRecording) wavRecorder?.write(buffer, read)
            }
        }.start()
    }

    private fun stopMic() {
        isMicOn = false
        recorder.stop()
        track.stop()
    }

    private fun simpleSeek(action: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) = action(p)
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
}
