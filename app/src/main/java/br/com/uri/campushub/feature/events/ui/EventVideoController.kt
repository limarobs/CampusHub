package br.com.uri.campushub.feature.events.ui

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsPolicy
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.UUID

internal class EventVideoController(
    private val activity: AppCompatActivity,
    @Suppress("unused") private val eventId: String,
    private val userId: String,
    private val authorName: String,
    private val policy: EventDetailsPolicy,
    private val mediaStore: LocalEventMediaStore
) {
    private lateinit var videosContainer: LinearLayout
    private lateinit var emptyVideosMessage: TextView
    private var pendingVideoFile: File? = null
    private val itemFactory = EventMediaItemFactory(activity)

    private val selectVideoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::copyVideo) }
    private val recordVideoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val videoFile = pendingVideoFile
        pendingVideoFile = null
        if (result.resultCode == Activity.RESULT_OK && videoFile?.isFile == true && videoFile.length() > 0) {
            addVideo(videoFile)
        } else {
            videoFile?.delete()
        }
    }
    private val videoCameraPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) recordVideo() }

    fun bind() {
        videosContainer = activity.findViewById(R.id.videosContainer)
        emptyVideosMessage = activity.findViewById(R.id.tvEmptyVideos)
        activity.findViewById<MaterialButton>(R.id.btnAddVideo)
            .setOnClickListener { showVideoOptions() }
        loadVideos()
    }

    fun release() {
        pendingVideoFile?.delete()
        pendingVideoFile = null
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
        ) recordVideo()
        else videoCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun recordVideo() {
        pendingVideoFile?.delete()
        pendingVideoFile = mediaStore.createFile("event_videos", "mp4")
        val outputUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            pendingVideoFile!!
        )
        recordVideoLauncher.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            clipData = ClipData.newRawUri("event_video", outputUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun copyVideo(sourceUri: android.net.Uri) {
        val videoFile = mediaStore.createFile("event_videos", "mp4")
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
        val videos = mediaStore.readMedia(VIDEO_PREFERENCES, "videos")
        videos.add(0, newMedia(videoFile))
        mediaStore.saveMedia(VIDEO_PREFERENCES, "videos", videos)
        loadVideos()
    }

    private fun loadVideos() {
        videosContainer.removeAllViews()
        val videos = mediaStore.readMedia(VIDEO_PREFERENCES, "videos").filter { File(it.path).isFile }
        emptyVideosMessage.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
        videos.forEach(::addVideoView)
    }

    private fun addVideoView(video: LocalEventMedia) {
        val item = itemFactory.item()
        item.addView(itemFactory.authorText(video.authorName))
        item.addView(itemFactory.dateText(video.createdAt))
        val videoView = VideoView(activity).apply {
            setVideoURI(android.net.Uri.fromFile(File(video.path)))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                itemFactory.dp(220)
            ).apply { topMargin = 12.dp() }
        }
        item.addView(videoView)
        item.addView(MaterialButton(activity).apply {
            text = "Assistir"
            setOnClickListener { videoView.start() }
        })
        if (policy.canManagePublication(video.authorId, userId)) {
            item.addView(itemFactory.deleteButton { deleteVideo(video) })
        }
        videosContainer.addView(item)
    }

    private fun deleteVideo(video: LocalEventMedia) {
        mediaStore.readMedia(VIDEO_PREFERENCES, "videos")
            .filterNot { it.id == video.id }
            .also { mediaStore.saveMedia(VIDEO_PREFERENCES, "videos", it) }
        File(video.path).delete()
        loadVideos()
    }

    private fun newMedia(file: File) = LocalEventMedia(
        UUID.randomUUID().toString(), userId, authorName, System.currentTimeMillis(), file.absolutePath
    )

    private fun Int.dp() = (this * activity.resources.displayMetrics.density).toInt()

    private companion object { const val VIDEO_PREFERENCES = "event_videos" }
}
