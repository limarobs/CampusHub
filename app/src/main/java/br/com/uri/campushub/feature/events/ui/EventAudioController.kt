package br.com.uri.campushub.feature.events.ui

import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsPolicy
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.UUID

internal class EventAudioController(
    private val activity: AppCompatActivity,
    @Suppress("unused") private val eventId: String,
    private val userId: String,
    private val authorName: String,
    private val policy: EventDetailsPolicy,
    private val mediaStore: LocalEventMediaStore
) {
    private lateinit var recordAudioButton: MaterialButton
    private lateinit var recordingTime: TextView
    private lateinit var audiosContainer: LinearLayout
    private lateinit var emptyAudiosMessage: TextView
    private var audioRecorder: MediaRecorder? = null
    private var audioPlayer: MediaPlayer? = null
    private var recordingFile: File? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var recordingStartedAt = 0L
    private var recordingTimer: Runnable? = null
    private var playingTimer: Runnable? = null
    private val itemFactory = EventMediaItemFactory(activity)

    private val audioPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startAudioRecording() }

    fun bind() {
        recordAudioButton = activity.findViewById(R.id.btnRecordAudio)
        recordingTime = activity.findViewById(R.id.tvRecordingTime)
        audiosContainer = activity.findViewById(R.id.audiosContainer)
        emptyAudiosMessage = activity.findViewById(R.id.tvEmptyAudios)
        recordAudioButton.setOnClickListener { requestOrToggleAudioRecording() }
        loadAudios()
    }

    fun release() {
        recordingTimer?.let(audioHandler::removeCallbacks)
        playingTimer?.let(audioHandler::removeCallbacks)
        audioRecorder?.release()
        audioPlayer?.release()
    }

    private fun requestOrToggleAudioRecording() {
        if (audioRecorder != null) {
            stopAudioRecording()
        } else if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startAudioRecording()
        } else {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startAudioRecording() {
        val file = mediaStore.createFile("event_audios", "m4a")
        try {
            audioRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(file.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                prepare()
                start()
            }
            recordingFile = file
            recordAudioButton.text = "Parar gravação"
            recordingStartedAt = System.currentTimeMillis()
            recordingTime.visibility = View.VISIBLE
            updateRecordingTime()
        } catch (_: Exception) {
            file.delete()
            audioRecorder?.release()
            audioRecorder = null
            Toast.makeText(activity, "Não foi possível iniciar a gravação.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() {
        val file = recordingFile
        try {
            audioRecorder?.stop()
            if (file != null) {
                val audios = mediaStore.readMedia(AUDIO_PREFERENCES, "audios")
                audios.add(0, newMedia(file))
                mediaStore.saveMedia(AUDIO_PREFERENCES, "audios", audios)
            }
        } catch (_: Exception) {
            file?.delete()
        } finally {
            audioRecorder?.release()
            audioRecorder = null
            recordingFile = null
            recordAudioButton.text = "Gravar áudio"
            recordingTimer?.let(audioHandler::removeCallbacks)
            recordingTime.visibility = View.GONE
            loadAudios()
        }
    }

    private fun updateRecordingTime() {
        recordingTime.text = "Gravando: ${policy.formatAudioTime(System.currentTimeMillis() - recordingStartedAt)}"
        recordingTimer = Runnable { updateRecordingTime() }
        audioHandler.postDelayed(recordingTimer!!, 500)
    }

    private fun loadAudios() {
        audiosContainer.removeAllViews()
        val audios = mediaStore.readMedia(AUDIO_PREFERENCES, "audios").filter { File(it.path).isFile }
        emptyAudiosMessage.visibility = if (audios.isEmpty()) View.VISIBLE else View.GONE
        audios.forEach(::addAudioView)
    }

    private fun addAudioView(audio: LocalEventMedia) {
        val item = itemFactory.item()
        item.addView(itemFactory.authorText(audio.authorName))
        item.addView(itemFactory.dateText(audio.createdAt))
        val progress = SeekBar(activity).apply { isEnabled = false; max = 1 }
        val duration = TextView(activity).apply {
            text = "0:00 / --:--"
            textSize = 12f
            setTextColor(activity.getColor(R.color.text_secondary))
        }
        val playButton = MaterialButton(activity).apply { text = "Reproduzir" }
        playButton.setOnClickListener { playAudio(audio.path, progress, duration, playButton) }
        item.addView(progress)
        item.addView(duration)
        item.addView(playButton)
        if (policy.canManagePublication(audio.authorId, userId)) {
            item.addView(itemFactory.deleteButton { deleteAudio(audio) })
        }
        audiosContainer.addView(item)
    }

    private fun deleteAudio(audio: LocalEventMedia) {
        mediaStore.readMedia(AUDIO_PREFERENCES, "audios")
            .filterNot { it.id == audio.id }
            .also { mediaStore.saveMedia(AUDIO_PREFERENCES, "audios", it) }
        File(audio.path).delete()
        loadAudios()
    }

    private fun playAudio(
        path: String,
        progress: SeekBar,
        durationText: TextView,
        playButton: MaterialButton
    ) {
        audioPlayer?.release()
        playingTimer?.let(audioHandler::removeCallbacks)
        audioPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            progress.max = duration
            durationText.text = "0:00 / ${policy.formatAudioTime(duration.toLong())}"
            playButton.text = "Reproduzindo"
            setOnCompletionListener {
                playingTimer?.let(audioHandler::removeCallbacks)
                progress.progress = 0
                durationText.text = "0:00 / ${policy.formatAudioTime(duration.toLong())}"
                playButton.text = "Reproduzir"
                release()
                audioPlayer = null
            }
            start()
        }
        updatePlayingTime(progress, durationText)
    }

    private fun updatePlayingTime(progress: SeekBar, durationText: TextView) {
        val player = audioPlayer ?: return
        progress.progress = player.currentPosition
        durationText.text = "${policy.formatAudioTime(player.currentPosition.toLong())} / " +
            policy.formatAudioTime(player.duration.toLong())
        playingTimer = Runnable { updatePlayingTime(progress, durationText) }
        audioHandler.postDelayed(playingTimer!!, 250)
    }

    private fun newMedia(file: File) = LocalEventMedia(
        UUID.randomUUID().toString(), userId, authorName, System.currentTimeMillis(), file.absolutePath
    )

    private companion object { const val AUDIO_PREFERENCES = "event_audios" }
}
