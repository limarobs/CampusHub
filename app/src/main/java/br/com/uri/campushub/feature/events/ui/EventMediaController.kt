package br.com.uri.campushub.feature.events.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsPolicy
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Owns local media capture, storage and rendering for one event. */
internal class EventMediaController(
    private val activity: AppCompatActivity,
    private val eventId: String,
    private val userId: String,
    private val authorName: String,
    private val policy: EventDetailsPolicy
) {
    private data class LocalPhoto(
        val id: String,
        val authorId: String,
        val authorName: String,
        val createdAt: Long,
        val path: String
    )

    private data class LocalAudio(
        val id: String,
        val authorId: String,
        val authorName: String,
        val createdAt: Long,
        val path: String
    )

    private data class LocalVideo(
        val id: String,
        val authorId: String,
        val authorName: String,
        val createdAt: Long,
        val path: String
    )

    private lateinit var photosContainer: LinearLayout
    private lateinit var emptyPhotosMessage: TextView
    private lateinit var videosContainer: LinearLayout
    private lateinit var emptyVideosMessage: TextView
    private lateinit var recordAudioButton: MaterialButton
    private lateinit var recordingTime: TextView
    private lateinit var audiosContainer: LinearLayout
    private lateinit var emptyAudiosMessage: TextView

    private var audioRecorder: MediaRecorder? = null
    private var audioPlayer: MediaPlayer? = null
    private var recordingFile: File? = null
    private val audioHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var recordingStartedAt = 0L
    private var recordingTimer: Runnable? = null
    private var playingTimer: Runnable? = null

    private val takePhotoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let(::saveCameraPhoto) }

    private val selectPhotoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::copyGalleryPhoto) }

    private val cameraPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) takePhotoLauncher.launch(null) }

    private val selectVideoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::copyVideo) }

    private val recordVideoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> result.data?.data?.let(::copyVideo) }

    private val videoCameraPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) recordVideo() }

    private val audioPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startAudioRecording() }

    fun bind() {
        photosContainer = activity.findViewById(R.id.photosContainer)
        emptyPhotosMessage = activity.findViewById(R.id.tvEmptyPhotos)
        activity.findViewById<MaterialButton>(R.id.btnAddPhoto)
            .setOnClickListener { showPhotoOptions() }
        videosContainer = activity.findViewById(R.id.videosContainer)
        emptyVideosMessage = activity.findViewById(R.id.tvEmptyVideos)
        activity.findViewById<MaterialButton>(R.id.btnAddVideo)
            .setOnClickListener { showVideoOptions() }
        recordAudioButton = activity.findViewById(R.id.btnRecordAudio)
        recordingTime = activity.findViewById(R.id.tvRecordingTime)
        audiosContainer = activity.findViewById(R.id.audiosContainer)
        emptyAudiosMessage = activity.findViewById(R.id.tvEmptyAudios)
        recordAudioButton.setOnClickListener { requestOrToggleAudioRecording() }

        loadPhotos()
        loadVideos()
        loadAudios()
    }

    fun release() {
        recordingTimer?.let(audioHandler::removeCallbacks)
        playingTimer?.let(audioHandler::removeCallbacks)
        audioRecorder?.release()
        audioPlayer?.release()
    }

    private fun showPhotoOptions() {
        AlertDialog.Builder(activity)
            .setTitle("Adicionar foto")
            .setItems(arrayOf("Tirar foto", "Escolher da galeria")) { _, option ->
                if (option == 0) requestCameraPhoto() else selectPhotoLauncher.launch("image/*")
            }
            .show()
    }

    private fun requestCameraPhoto() {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            takePhotoLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun saveCameraPhoto(bitmap: Bitmap) {
        val photoFile = createPhotoFile()
        if (photoFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }) {
            addPhoto(photoFile)
        } else {
            photoFile.delete()
        }
    }

    private fun copyGalleryPhoto(sourceUri: android.net.Uri) {
        val photoFile = createPhotoFile()
        try {
            activity.contentResolver.openInputStream(sourceUri)?.use { input ->
                photoFile.outputStream().use(input::copyTo)
            } ?: error("Imagem indisponível")
            addPhoto(photoFile)
        } catch (_: Exception) {
            photoFile.delete()
            Toast.makeText(activity, "Não foi possível adicionar a foto.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPhotoFile(): File {
        val directory = File(activity.filesDir, "event_photos").apply { mkdirs() }
        return File(directory, "${UUID.randomUUID()}.jpg")
    }

    private fun addPhoto(photoFile: File) {
        val photos = readPhotos()
        photos.add(0, LocalPhoto(UUID.randomUUID().toString(), userId, authorName,
            System.currentTimeMillis(), photoFile.absolutePath))
        savePhotos(photos)
        loadPhotos()
    }

    private fun loadPhotos() {
        photosContainer.removeAllViews()
        val photos = readPhotos().filter { File(it.path).isFile }
        emptyPhotosMessage.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        photos.forEach(::addPhotoView)
    }

    private fun addPhotoView(photo: LocalPhoto) {
        val item = mediaItem()
        item.addView(authorText(photo.authorName))
        item.addView(dateText(photo.createdAt))
        item.addView(ImageView(activity).apply {
            setImageURI(android.net.Uri.fromFile(File(photo.path)))
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Foto publicada no evento"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 220.dp())
                .apply { topMargin = 12.dp() }
        })
        if (policy.canManagePublication(photo.authorId, userId)) {
            item.addView(deleteButton { deletePhoto(photo) })
        }
        photosContainer.addView(item)
    }

    private fun deletePhoto(photo: LocalPhoto) {
        readPhotos().filterNot { it.id == photo.id }.also(::savePhotos)
        File(photo.path).delete()
        loadPhotos()
    }

    private fun readPhotos(): MutableList<LocalPhoto> = try {
        val json = JSONArray(activity.getSharedPreferences(PHOTO_PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .getString("photos_$eventId", "[]"))
        MutableList(json.length()) { index -> json.getJSONObject(index).let {
            LocalPhoto(it.getString("id"), it.getString("authorId"), it.getString("authorName"),
                it.getLong("createdAt"), it.getString("path"))
        } }
    } catch (_: Exception) { mutableListOf() }

    private fun savePhotos(photos: List<LocalPhoto>) {
        val json = JSONArray()
        photos.forEach { photo -> json.put(JSONObject().put("id", photo.id)
            .put("authorId", photo.authorId).put("authorName", photo.authorName)
            .put("createdAt", photo.createdAt).put("path", photo.path)) }
        activity.getSharedPreferences(PHOTO_PREFERENCES, AppCompatActivity.MODE_PRIVATE).edit()
            .putString("photos_$eventId", json.toString()).apply()
    }

    private fun showVideoOptions() {
        AlertDialog.Builder(activity)
            .setTitle("Adicionar vídeo")
            .setItems(arrayOf("Gravar vídeo", "Escolher da galeria")) { _, option ->
                if (option == 0) requestCameraVideo() else selectVideoLauncher.launch("video/*")
            }
            .show()
    }

    private fun requestCameraVideo() {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            recordVideo()
        } else {
            videoCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun recordVideo() {
        recordVideoLauncher.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)
        })
    }

    private fun copyVideo(sourceUri: android.net.Uri) {
        val videoFile = File(File(activity.filesDir, "event_videos").apply { mkdirs() },
            "${UUID.randomUUID()}.mp4")
        try {
            activity.contentResolver.openInputStream(sourceUri)?.use { input ->
                videoFile.outputStream().use(input::copyTo)
            } ?: error("Vídeo indisponível")
            addVideo(videoFile)
        } catch (_: Exception) {
            videoFile.delete()
            Toast.makeText(activity, "Não foi possível adicionar o vídeo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addVideo(videoFile: File) {
        val videos = readVideos()
        videos.add(0, LocalVideo(UUID.randomUUID().toString(), userId, authorName,
            System.currentTimeMillis(), videoFile.absolutePath))
        saveVideos(videos)
        loadVideos()
    }

    private fun loadVideos() {
        videosContainer.removeAllViews()
        val videos = readVideos().filter { File(it.path).isFile }
        emptyVideosMessage.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
        videos.forEach(::addVideoView)
    }

    private fun addVideoView(video: LocalVideo) {
        val item = mediaItem()
        item.addView(authorText(video.authorName))
        item.addView(dateText(video.createdAt))
        val videoView = VideoView(activity).apply {
            setVideoURI(android.net.Uri.fromFile(File(video.path)))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 220.dp())
                .apply { topMargin = 12.dp() }
        }
        item.addView(videoView)
        item.addView(MaterialButton(activity).apply {
            text = "Assistir"
            setOnClickListener { videoView.start() }
        })
        if (policy.canManagePublication(video.authorId, userId)) {
            item.addView(deleteButton { deleteVideo(video) })
        }
        videosContainer.addView(item)
    }

    private fun deleteVideo(video: LocalVideo) {
        readVideos().filterNot { it.id == video.id }.also(::saveVideos)
        File(video.path).delete()
        loadVideos()
    }

    private fun readVideos(): MutableList<LocalVideo> = try {
        val json = JSONArray(activity.getSharedPreferences(VIDEO_PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .getString("videos_$eventId", "[]"))
        MutableList(json.length()) { index -> json.getJSONObject(index).let {
            LocalVideo(it.getString("id"), it.getString("authorId"), it.getString("authorName"),
                it.getLong("createdAt"), it.getString("path"))
        } }
    } catch (_: Exception) { mutableListOf() }

    private fun saveVideos(videos: List<LocalVideo>) {
        val json = JSONArray()
        videos.forEach { video -> json.put(JSONObject().put("id", video.id)
            .put("authorId", video.authorId).put("authorName", video.authorName)
            .put("createdAt", video.createdAt).put("path", video.path)) }
        activity.getSharedPreferences(VIDEO_PREFERENCES, AppCompatActivity.MODE_PRIVATE).edit()
            .putString("videos_$eventId", json.toString()).apply()
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
        val file = File(activity.filesDir, "event_audios").apply { mkdirs() }
            .let { File(it, "${UUID.randomUUID()}.m4a") }
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
                val audios = readAudios()
                audios.add(0, LocalAudio(UUID.randomUUID().toString(), userId, authorName,
                    System.currentTimeMillis(), file.absolutePath))
                saveAudios(audios)
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
        val audios = readAudios().filter { File(it.path).isFile }
        emptyAudiosMessage.visibility = if (audios.isEmpty()) View.VISIBLE else View.GONE
        audios.forEach(::addAudioView)
    }

    private fun addAudioView(audio: LocalAudio) {
        val item = mediaItem()
        item.addView(authorText(audio.authorName))
        item.addView(dateText(audio.createdAt))
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
            item.addView(deleteButton { deleteAudio(audio) })
        }
        audiosContainer.addView(item)
    }

    private fun deleteAudio(audio: LocalAudio) {
        readAudios().filterNot { it.id == audio.id }.also(::saveAudios)
        File(audio.path).delete()
        loadAudios()
    }

    private fun playAudio(path: String, progress: SeekBar, durationText: TextView, playButton: MaterialButton) {
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
        durationText.text = "${policy.formatAudioTime(player.currentPosition.toLong())} / ${policy.formatAudioTime(player.duration.toLong())}"
        playingTimer = Runnable { updatePlayingTime(progress, durationText) }
        audioHandler.postDelayed(playingTimer!!, 250)
    }

    private fun readAudios(): MutableList<LocalAudio> = try {
        val json = JSONArray(activity.getSharedPreferences(AUDIO_PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .getString("audios_$eventId", "[]"))
        MutableList(json.length()) { index -> json.getJSONObject(index).let {
            LocalAudio(it.getString("id"), it.getString("authorId"), it.getString("authorName"),
                it.getLong("createdAt"), it.getString("path"))
        } }
    } catch (_: Exception) { mutableListOf() }

    private fun saveAudios(audios: List<LocalAudio>) {
        val json = JSONArray()
        audios.forEach { audio -> json.put(JSONObject().put("id", audio.id)
            .put("authorId", audio.authorId).put("authorName", audio.authorName)
            .put("createdAt", audio.createdAt).put("path", audio.path)) }
        activity.getSharedPreferences(AUDIO_PREFERENCES, AppCompatActivity.MODE_PRIVATE).edit()
            .putString("audios_$eventId", json.toString()).apply()
    }

    private fun mediaItem() = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
        setBackgroundColor(activity.getColor(R.color.surface))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 12.dp() }
    }

    private fun authorText(author: String) = TextView(activity).apply {
        text = author
        textSize = 16f
        setTextColor(activity.getColor(R.color.text_primary))
    }

    private fun dateText(createdAt: Long) = TextView(activity).apply {
        text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(createdAt))
        textSize = 12f
        setTextColor(activity.getColor(R.color.text_secondary))
    }

    private fun deleteButton(onDelete: () -> Unit) = MaterialButton(activity).apply {
        text = "Excluir"
        setOnClickListener { onDelete() }
    }

    private fun Int.dp() = (this * activity.resources.displayMetrics.density).toInt()

    private companion object {
        const val PHOTO_PREFERENCES = "event_photos"
        const val AUDIO_PREFERENCES = "event_audios"
        const val VIDEO_PREFERENCES = "event_videos"
    }
}
