package br.com.uri.campushub.feature.events.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.provider.MediaStore
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.data.EventCommentRepository
import br.com.uri.campushub.feature.events.model.EventComment
import br.com.uri.campushub.feature.events.model.CampusEvent
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.text.DateFormat
import java.io.File
import java.util.Date
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class EventDetailsActivity : AppCompatActivity() {

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

    private lateinit var registrationButton: MaterialButton
    private lateinit var favoriteButton: MaterialButton
    private lateinit var ratingBar: RatingBar
    private lateinit var saveRatingButton: MaterialButton
    private lateinit var ratingSummary: TextView
    private lateinit var ratingEligibility: TextView
    private lateinit var commentInput: TextInputEditText
    private lateinit var addCommentButton: MaterialButton
    private lateinit var commentsProgress: ProgressBar
    private lateinit var commentsContainer: LinearLayout
    private lateinit var emptyCommentsMessage: TextView
    private lateinit var commentsErrorMessage: TextView
    private lateinit var photosContainer: LinearLayout
    private lateinit var emptyPhotosMessage: TextView
    private lateinit var videosContainer: LinearLayout
    private lateinit var emptyVideosMessage: TextView
    private lateinit var recordAudioButton: MaterialButton
    private lateinit var recordingTime: TextView
    private lateinit var audiosContainer: LinearLayout
    private lateinit var emptyAudiosMessage: TextView
    private lateinit var authorName: String
    private lateinit var userId: String
    private lateinit var event: CampusEvent
    private val detailsViewModel: EventDetailsViewModel by viewModels()
    private var audioRecorder: MediaRecorder? = null
    private var audioPlayer: MediaPlayer? = null
    private var recordingFile: File? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var recordingStartedAt = 0L
    private var recordingTimer: Runnable? = null
    private var playingTimer: Runnable? = null

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) saveCameraPhoto(bitmap)
    }

    private val selectPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(::copyGalleryPhoto)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePhotoLauncher.launch(null)
    }

    private val selectVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(::copyVideo)
    }

    private val recordVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let(::copyVideo)
    }

    private val videoCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recordVideo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        userId = currentUser.uid
        authorName = currentUser.displayName ?: currentUser.email ?: "Aluno"
        event = CampusEvent(
            id = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
            date = intent.getStringExtra(EXTRA_DATE).orEmpty(),
            location = intent.getStringExtra(EXTRA_LOCATION).orEmpty(),
            address = intent.getStringExtra(EXTRA_ADDRESS).orEmpty(),
            latitude = coordinateFromExtra(EXTRA_LATITUDE),
            longitude = coordinateFromExtra(EXTRA_LONGITUDE),
            category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Sem categoria",
            startsAt = timestampFromExtras(EXTRA_STARTS_AT_SECONDS, EXTRA_STARTS_AT_NANOSECONDS),
            endsAt = timestampFromExtras(EXTRA_ENDS_AT_SECONDS, EXTRA_ENDS_AT_NANOSECONDS)
        )

        setContentView(R.layout.activity_event_details)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvEventDetailsTitle).text =
            event.title
        findViewById<TextView>(R.id.tvEventDetailsDescription).text =
            event.description
        findViewById<TextView>(R.id.tvEventDetailsDate).text =
            event.date
        findViewById<TextView>(R.id.tvEventDetailsLocation).text =
            event.location
        findViewById<TextView>(R.id.tvEventDetailsAddress).text =
            event.address.ifBlank { "Endereço não informado." }
        showEventMap()

        registrationButton = findViewById(R.id.btnRegistration)
        registrationButton.setOnClickListener { detailsViewModel.toggleRegistration() }

        favoriteButton = findViewById(R.id.btnFavorite)
        favoriteButton.setOnClickListener { detailsViewModel.toggleFavorite() }

        photosContainer = findViewById(R.id.photosContainer)
        emptyPhotosMessage = findViewById(R.id.tvEmptyPhotos)
        findViewById<MaterialButton>(R.id.btnAddPhoto).setOnClickListener { showPhotoOptions() }
        videosContainer = findViewById(R.id.videosContainer)
        emptyVideosMessage = findViewById(R.id.tvEmptyVideos)
        findViewById<MaterialButton>(R.id.btnAddVideo).setOnClickListener { showVideoOptions() }
        recordAudioButton = findViewById(R.id.btnRecordAudio)
        recordingTime = findViewById(R.id.tvRecordingTime)
        audiosContainer = findViewById(R.id.audiosContainer)
        emptyAudiosMessage = findViewById(R.id.tvEmptyAudios)
        recordAudioButton.setOnClickListener { requestOrToggleAudioRecording() }

        ratingBar = findViewById(R.id.ratingBar)
        saveRatingButton = findViewById(R.id.btnSaveRating)
        ratingSummary = findViewById(R.id.tvRatingSummary)
        ratingEligibility = findViewById(R.id.tvRatingEligibility)
        ratingBar.isEnabled = false
        saveRatingButton.isEnabled = false
        saveRatingButton.setOnClickListener { detailsViewModel.saveRating(ratingBar.rating.toInt()) }

        commentInput = findViewById(R.id.etNewComment)
        addCommentButton = findViewById(R.id.btnAddComment)
        commentsProgress = findViewById(R.id.progressComments)
        commentsContainer = findViewById(R.id.commentsContainer)
        emptyCommentsMessage = findViewById(R.id.tvEmptyComments)
        commentsErrorMessage = findViewById(R.id.tvCommentsError)
        addCommentButton.setOnClickListener { addComment() }

        observeDetailsState()
        detailsViewModel.start(userId, event)
        loadPhotos()
        loadVideos()
        loadAudios()
        loadComments()
    }

    private fun observeDetailsState() {
        detailsViewModel.uiState.observe(this) { state ->
            registrationButton.text = state.registrationText
            registrationButton.isEnabled = state.registrationEnabled
            favoriteButton.text = state.favoriteText
            favoriteButton.isEnabled = state.favoriteEnabled
            ratingSummary.text = state.ratingSummary
            ratingEligibility.text = state.ratingEligibility
            ratingBar.rating = state.rating
            ratingBar.isEnabled = state.ratingEnabled
            saveRatingButton.text = state.ratingButtonText
            saveRatingButton.isEnabled = state.ratingButtonEnabled
        }
        detailsViewModel.message.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /* Legacy Firestore callbacks moved to EventDetailsViewModel.
    private fun loadRatingsSummary() {
        ratingSummary.text = "Carregando avaliações..."
        EventRatingRepository.getRatings(event.id).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                ratingSummary.text = "Não foi possível carregar as avaliações."
                return@addOnCompleteListener
            }

            val ratings = task.result?.documents.orEmpty()
                .mapNotNull { document -> document.getLong("rating")?.toInt() }
                .filter { rating -> rating in 1..5 }

            ratingSummary.text = if (ratings.isEmpty()) {
                "Ainda não há avaliações para este evento."
            } else {
                val average = ratings.average()
                val countText = if (ratings.size == 1) "avaliação" else "avaliações"
                "Média: %.1f de 5 (%d %s)".format(average, ratings.size, countText)
            }
        }
    }

    private fun updateRatingEligibility() {
        if (!hasEventEnded()) {
            ratingEligibility.text = "As avaliações ficam disponíveis após o encerramento do evento."
            ratingBar.isEnabled = false
            saveRatingButton.isEnabled = false
            return
        }

        if (!isRegistrationStateLoaded) {
            ratingEligibility.text = "Verificando sua inscrição para liberar a avaliação..."
            ratingBar.isEnabled = false
            saveRatingButton.isEnabled = false
            return
        }

        if (!isRegistered) {
            ratingEligibility.text = "Apenas alunos inscritos podem avaliar este evento."
            ratingBar.isEnabled = false
            saveRatingButton.isEnabled = false
            return
        }

        ratingEligibility.text = "Escolha uma nota de 1 a 5."
        loadUserRating()
    }

    private fun loadUserRating() {
        isUserRatingLoaded = false
        ratingBar.isEnabled = false
        saveRatingButton.isEnabled = false
        saveRatingButton.text = "Carregando sua avaliação..."

        EventRatingRepository.getRating(event.id, userId).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                ratingEligibility.text = "Não foi possível verificar sua avaliação."
                saveRatingButton.text = "Tentar novamente"
                saveRatingButton.isEnabled = true
                saveRatingButton.setOnClickListener { loadUserRating() }
                return@addOnCompleteListener
            }

            val currentRating = task.result?.getLong("rating")?.toInt()
            if (currentRating != null && currentRating in 1..5) {
                ratingBar.rating = currentRating?.toFloat() ?: 0f
                saveRatingButton.text = "Atualizar avaliação"
            } else {
                ratingBar.rating = 0f
                saveRatingButton.text = "Publicar avaliação"
            }

            isUserRatingLoaded = true
            ratingBar.isEnabled = true
            saveRatingButton.isEnabled = true
            saveRatingButton.setOnClickListener { saveRating() }
        }
    }

    private fun saveRating() {
        if (!isUserRatingLoaded) {
            loadUserRating()
            return
        }

        val rating = ratingBar.rating.toInt()
        if (rating !in 1..5) {
            Toast.makeText(this, "Escolha uma nota de 1 a 5.", Toast.LENGTH_SHORT).show()
            return
        }

        ratingBar.isEnabled = false
        saveRatingButton.isEnabled = false
        saveRatingButton.text = "Salvando avaliação..."
        EventRatingRepository.saveRating(event.id, userId, rating).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Avaliação salva.", Toast.LENGTH_SHORT).show()
                loadRatingsSummary()
                loadUserRating()
            } else {
                Toast.makeText(
                    this,
                    "Não foi possível salvar sua avaliação.",
                    Toast.LENGTH_SHORT
                ).show()
                loadUserRating()
            }
        }
    }

    private fun hasEventEnded(): Boolean {
        return event.endsAt?.toDate()?.before(Date()) == true
    }
    */

    private fun showPhotoOptions() {
        AlertDialog.Builder(this)
            .setTitle("Adicionar foto")
            .setItems(arrayOf("Tirar foto", "Escolher da galeria")) { _, option ->
                if (option == 0) requestCameraPhoto() else selectPhotoLauncher.launch("image/*")
            }
            .show()
    }

    private fun requestCameraPhoto() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
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
            contentResolver.openInputStream(sourceUri)?.use { input ->
                photoFile.outputStream().use(input::copyTo)
            } ?: error("Imagem indisponível")
            addPhoto(photoFile)
        } catch (_: Exception) {
            photoFile.delete()
            Toast.makeText(this, "Não foi possível adicionar a foto.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPhotoFile(): File {
        val directory = File(filesDir, "event_photos").apply { mkdirs() }
        return File(directory, "${UUID.randomUUID()}.jpg")
    }

    private fun addPhoto(photoFile: File) {
        val photos = readPhotos()
        photos.add(
            0,
            LocalPhoto(
                id = UUID.randomUUID().toString(),
                authorId = userId,
                authorName = authorName,
                createdAt = System.currentTimeMillis(),
                path = photoFile.absolutePath
            )
        )
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
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
            setBackgroundColor(getColor(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp() }
        }
        item.addView(TextView(this).apply {
            text = photo.authorName
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
        })
        item.addView(TextView(this).apply {
            text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(photo.createdAt))
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
        })
        item.addView(ImageView(this).apply {
            setImageURI(android.net.Uri.fromFile(File(photo.path)))
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Foto publicada no evento"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                220.dp()
            ).apply { topMargin = 12.dp() }
        })
        if (photo.authorId == userId) {
            item.addView(MaterialButton(this).apply {
                text = "Excluir"
                setOnClickListener { deletePhoto(photo) }
            })
        }
        photosContainer.addView(item)
    }

    private fun deletePhoto(photo: LocalPhoto) {
        readPhotos().filterNot { it.id == photo.id }.also(::savePhotos)
        File(photo.path).delete()
        loadPhotos()
    }

    private fun readPhotos(): MutableList<LocalPhoto> {
        return try {
            val saved = getSharedPreferences(PHOTO_PREFERENCES, MODE_PRIVATE)
                .getString("photos_${event.id}", "[]")
            val json = JSONArray(saved)
            MutableList(json.length()) { index ->
                json.getJSONObject(index).let {
                    LocalPhoto(
                        it.getString("id"),
                        it.getString("authorId"),
                        it.getString("authorName"),
                        it.getLong("createdAt"),
                        it.getString("path")
                    )
                }
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun savePhotos(photos: List<LocalPhoto>) {
        val json = JSONArray()
        photos.forEach { photo ->
            json.put(
                JSONObject()
                    .put("id", photo.id)
                    .put("authorId", photo.authorId)
                    .put("authorName", photo.authorName)
                    .put("createdAt", photo.createdAt)
                    .put("path", photo.path)
            )
        }
        getSharedPreferences(PHOTO_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putString("photos_${event.id}", json.toString())
            .apply()
    }

    private fun showVideoOptions() {
        AlertDialog.Builder(this)
            .setTitle("Adicionar vídeo")
            .setItems(arrayOf("Gravar vídeo", "Escolher da galeria")) { _, option ->
                if (option == 0) requestCameraVideo() else selectVideoLauncher.launch("video/*")
            }
            .show()
    }

    private fun requestCameraVideo() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
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
        val videoFile = File(File(filesDir, "event_videos").apply { mkdirs() }, "${UUID.randomUUID()}.mp4")
        try {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                videoFile.outputStream().use(input::copyTo)
            } ?: error("Vídeo indisponível")
            addVideo(videoFile)
        } catch (_: Exception) {
            videoFile.delete()
            Toast.makeText(this, "Não foi possível adicionar o vídeo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addVideo(videoFile: File) {
        val videos = readVideos()
        videos.add(0, LocalVideo(UUID.randomUUID().toString(), userId, authorName, System.currentTimeMillis(), videoFile.absolutePath))
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
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
            setBackgroundColor(getColor(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = 12.dp() }
        }
        item.addView(TextView(this).apply {
            text = video.authorName
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
        })
        item.addView(TextView(this).apply {
            text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(video.createdAt))
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
        })
        val videoView = VideoView(this).apply {
            setVideoURI(android.net.Uri.fromFile(File(video.path)))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 220.dp()).apply { topMargin = 12.dp() }
        }
        item.addView(videoView)
        item.addView(MaterialButton(this).apply {
            text = "Assistir"
            setOnClickListener { videoView.start() }
        })
        if (video.authorId == userId) {
            item.addView(MaterialButton(this).apply {
                text = "Excluir"
                setOnClickListener {
                    readVideos().filterNot { it.id == video.id }.also(::saveVideos)
                    File(video.path).delete()
                    loadVideos()
                }
            })
        }
        videosContainer.addView(item)
    }

    private fun readVideos(): MutableList<LocalVideo> = try {
        val json = JSONArray(getSharedPreferences(VIDEO_PREFERENCES, MODE_PRIVATE).getString("videos_${event.id}", "[]"))
        MutableList(json.length()) { index -> json.getJSONObject(index).let {
            LocalVideo(it.getString("id"), it.getString("authorId"), it.getString("authorName"),
                it.getLong("createdAt"), it.getString("path"))
        } }
    } catch (_: Exception) { mutableListOf() }

    private fun saveVideos(videos: List<LocalVideo>) {
        val json = JSONArray()
        videos.forEach { video -> json.put(JSONObject()
            .put("id", video.id).put("authorId", video.authorId).put("authorName", video.authorName)
            .put("createdAt", video.createdAt).put("path", video.path)) }
        getSharedPreferences(VIDEO_PREFERENCES, MODE_PRIVATE).edit()
            .putString("videos_${event.id}", json.toString()).apply()
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    private fun requestOrToggleAudioRecording() {
        if (audioRecorder != null) {
            stopAudioRecording()
        } else if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startAudioRecording()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }

    private fun startAudioRecording() {
        val file = File(filesDir, "event_audios").apply { mkdirs() }
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
            Toast.makeText(this, "Não foi possível iniciar a gravação.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() {
        val file = recordingFile
        try {
            audioRecorder?.stop()
            if (file != null) {
                val audios = readAudios()
                audios.add(0, LocalAudio(UUID.randomUUID().toString(), userId, authorName, System.currentTimeMillis(), file.absolutePath))
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
        recordingTime.text = "Gravando: ${formatAudioTime(System.currentTimeMillis() - recordingStartedAt)}"
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
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
            setBackgroundColor(getColor(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp() }
        }
        item.addView(TextView(this).apply {
            text = audio.authorName
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
        })
        item.addView(TextView(this).apply {
            text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(audio.createdAt))
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
        })
        val progress = SeekBar(this).apply {
            isEnabled = false
            max = 1
        }
        val duration = TextView(this).apply {
            text = "0:00 / --:--"
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
        }
        val playButton = MaterialButton(this).apply {
            text = "Reproduzir"
        }
        playButton.setOnClickListener { playAudio(audio.path, progress, duration, playButton) }
        item.addView(progress)
        item.addView(duration)
        item.addView(playButton)
        if (audio.authorId == userId) {
            item.addView(MaterialButton(this).apply {
                text = "Excluir"
                setOnClickListener {
                    readAudios().filterNot { it.id == audio.id }.also(::saveAudios)
                    File(audio.path).delete()
                    loadAudios()
                }
            })
        }
        audiosContainer.addView(item)
    }

    private fun playAudio(path: String, progress: SeekBar, durationText: TextView, playButton: MaterialButton) {
        audioPlayer?.release()
        playingTimer?.let(audioHandler::removeCallbacks)
        audioPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            progress.max = duration
            durationText.text = "0:00 / ${formatAudioTime(duration.toLong())}"
            playButton.text = "Reproduzindo"
            setOnCompletionListener {
                audioHandler.removeCallbacks(playingTimer!!)
                progress.progress = 0
                durationText.text = "0:00 / ${formatAudioTime(duration.toLong())}"
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
        durationText.text = "${formatAudioTime(player.currentPosition.toLong())} / ${formatAudioTime(player.duration.toLong())}"
        playingTimer = Runnable { updatePlayingTime(progress, durationText) }
        audioHandler.postDelayed(playingTimer!!, 250)
    }

    private fun formatAudioTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun readAudios(): MutableList<LocalAudio> = try {
        val json = JSONArray(getSharedPreferences(AUDIO_PREFERENCES, MODE_PRIVATE)
            .getString("audios_${event.id}", "[]"))
        MutableList(json.length()) { index -> json.getJSONObject(index).let {
            LocalAudio(it.getString("id"), it.getString("authorId"), it.getString("authorName"),
                it.getLong("createdAt"), it.getString("path"))
        } }
    } catch (_: Exception) { mutableListOf() }

    private fun saveAudios(audios: List<LocalAudio>) {
        val json = JSONArray()
        audios.forEach { audio -> json.put(JSONObject()
            .put("id", audio.id).put("authorId", audio.authorId).put("authorName", audio.authorName)
            .put("createdAt", audio.createdAt).put("path", audio.path)) }
        getSharedPreferences(AUDIO_PREFERENCES, MODE_PRIVATE).edit()
            .putString("audios_${event.id}", json.toString()).apply()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startAudioRecording()
        }
    }

    override fun onDestroy() {
        recordingTimer?.let(audioHandler::removeCallbacks)
        playingTimer?.let(audioHandler::removeCallbacks)
        audioRecorder?.release()
        audioPlayer?.release()
        super.onDestroy()
    }

    private fun loadComments() {
        commentsProgress.visibility = View.VISIBLE
        commentsContainer.removeAllViews()
        emptyCommentsMessage.visibility = View.GONE
        commentsErrorMessage.visibility = View.GONE

        EventCommentRepository.getComments(event.id).addOnCompleteListener { task ->
            commentsProgress.visibility = View.GONE

            if (!task.isSuccessful) {
                commentsErrorMessage.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            val comments = task.result?.documents.orEmpty().map(::toEventComment)
            if (comments.isEmpty()) {
                emptyCommentsMessage.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            comments.forEach(::addCommentCard)
        }
    }

    private fun addComment() {
        val content = commentInput.text?.toString()?.trim().orEmpty()
        if (content.isBlank()) {
            commentInput.error = "Digite um comentário antes de publicar."
            return
        }

        addCommentButton.isEnabled = false
        addCommentButton.text = "Publicando comentário..."
        EventCommentRepository.addComment(event.id, userId, authorName, content)
            .addOnCompleteListener { task ->
                addCommentButton.isEnabled = true
                addCommentButton.text = "Publicar comentário"

                if (task.isSuccessful) {
                    commentInput.text?.clear()
                    loadComments()
                } else {
                    Toast.makeText(
                        this,
                        "Não foi possível publicar o comentário.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun toEventComment(document: DocumentSnapshot): EventComment {
        return EventComment(
            id = document.id,
            authorId = document.getString("authorId").orEmpty(),
            authorName = document.getString("authorName") ?: "Aluno",
            content = document.getString("content").orEmpty(),
            createdAt = document.getTimestamp("createdAt")
        )
    }

    private fun addCommentCard(comment: EventComment) {
        val card = layoutInflater.inflate(R.layout.item_event_comment, commentsContainer, false)
        card.findViewById<TextView>(R.id.tvCommentAuthor).text = comment.authorName
        card.findViewById<TextView>(R.id.tvCommentDate).text = formatCommentDate(comment.createdAt)
        card.findViewById<TextView>(R.id.tvCommentContent).text = comment.content

        if (comment.authorId == userId) {
            val ownerActions = card.findViewById<LinearLayout>(R.id.commentOwnerActions)
            ownerActions.visibility = View.VISIBLE
            card.findViewById<MaterialButton>(R.id.btnEditComment).setOnClickListener {
                showEditCommentDialog(comment)
            }
            card.findViewById<MaterialButton>(R.id.btnDeleteComment).setOnClickListener {
                showDeleteCommentDialog(comment)
            }
        }

        commentsContainer.addView(card)
    }

    private fun showEditCommentDialog(comment: EventComment) {
        val editText = EditText(this).apply {
            setText(comment.content)
            setSelectAllOnFocus(false)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(48, 0, 48, 0)
        }

        AlertDialog.Builder(this)
            .setTitle("Editar comentário")
            .setView(editText)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isBlank()) {
                    Toast.makeText(this, "O comentário não pode ficar vazio.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                EventCommentRepository.updateComment(event.id, comment.id, content)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            loadComments()
                        } else {
                            Toast.makeText(
                                this,
                                "Não foi possível editar o comentário.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            .show()
    }

    private fun showDeleteCommentDialog(comment: EventComment) {
        AlertDialog.Builder(this)
            .setTitle("Excluir comentário?")
            .setMessage("Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                EventCommentRepository.deleteComment(event.id, comment.id)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            loadComments()
                        } else {
                            Toast.makeText(
                                this,
                                "Não foi possível excluir o comentário.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            .show()
    }

    private fun formatCommentDate(timestamp: Timestamp?): String {
        if (timestamp == null) {
            return "Agora"
        }

        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(timestamp.toDate().time))
    }

    private fun timestampFromExtras(secondsKey: String, nanosecondsKey: String): Timestamp? {
        val seconds = intent.getLongExtra(secondsKey, NO_TIMESTAMP)
        if (seconds == NO_TIMESTAMP) {
            return null
        }

        return Timestamp(seconds, intent.getIntExtra(nanosecondsKey, 0))
    }

    private fun coordinateFromExtra(key: String): Double? {
        return intent.getDoubleExtra(key, Double.NaN).takeUnless(Double::isNaN)
    }

    private fun showEventMap() {
        val mapContainer = findViewById<View>(R.id.eventMapContainer)
        if (!event.hasValidCoordinates()) {
            mapContainer.visibility = View.GONE
            return
        }

        val eventPoint = Point.fromLngLat(event.longitude!!, event.latitude!!)
        val mapView = findViewById<MapView>(R.id.eventMapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            mapView.annotations.createPointAnnotationManager().create(
                PointAnnotationOptions()
                    .withPoint(eventPoint)
                    .withTextField(event.title)
            )
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(eventPoint)
                    .zoom(16.0)
                    .build()
            )
        }
    }

    private fun CampusEvent.hasValidCoordinates(): Boolean {
        val eventLatitude = latitude ?: return false
        val eventLongitude = longitude ?: return false
        return eventLatitude in -90.0..90.0 && eventLongitude in -180.0..180.0
    }

    private fun navigateToLogin() {
        val loginIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish()
    }

    companion object {
        private const val PHOTO_PREFERENCES = "event_photos"
        private const val AUDIO_PREFERENCES = "event_audios"
        private const val VIDEO_PREFERENCES = "event_videos"
        private const val REQUEST_RECORD_AUDIO = 2
        private const val EXTRA_EVENT_ID = "extra_event_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_DESCRIPTION = "extra_description"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_LOCATION = "extra_location"
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_LATITUDE = "extra_latitude"
        private const val EXTRA_LONGITUDE = "extra_longitude"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_STARTS_AT_SECONDS = "extra_starts_at_seconds"
        private const val EXTRA_STARTS_AT_NANOSECONDS = "extra_starts_at_nanoseconds"
        private const val EXTRA_ENDS_AT_SECONDS = "extra_ends_at_seconds"
        private const val EXTRA_ENDS_AT_NANOSECONDS = "extra_ends_at_nanoseconds"
        private const val NO_TIMESTAMP = Long.MIN_VALUE

        fun createIntent(context: Context, event: CampusEvent): Intent {
            return Intent(context, EventDetailsActivity::class.java).apply {
                putExtra(EXTRA_EVENT_ID, event.id)
                putExtra(EXTRA_TITLE, event.title)
                putExtra(EXTRA_DESCRIPTION, event.description)
                putExtra(EXTRA_DATE, event.date)
                putExtra(EXTRA_LOCATION, event.location)
                putExtra(EXTRA_ADDRESS, event.address)
                putExtra(EXTRA_LATITUDE, event.latitude ?: Double.NaN)
                putExtra(EXTRA_LONGITUDE, event.longitude ?: Double.NaN)
                putExtra(EXTRA_CATEGORY, event.category)
                putExtra(EXTRA_STARTS_AT_SECONDS, event.startsAt?.seconds ?: NO_TIMESTAMP)
                putExtra(EXTRA_STARTS_AT_NANOSECONDS, event.startsAt?.nanoseconds ?: 0)
                putExtra(EXTRA_ENDS_AT_SECONDS, event.endsAt?.seconds ?: NO_TIMESTAMP)
                putExtra(EXTRA_ENDS_AT_NANOSECONDS, event.endsAt?.nanoseconds ?: 0)
            }
        }
    }
}
