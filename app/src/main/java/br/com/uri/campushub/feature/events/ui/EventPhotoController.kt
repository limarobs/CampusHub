package br.com.uri.campushub.feature.events.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsPolicy
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.UUID

internal class EventPhotoController(
    private val activity: AppCompatActivity,
    @Suppress("unused") private val eventId: String,
    private val userId: String,
    private val authorName: String,
    private val policy: EventDetailsPolicy,
    private val mediaStore: LocalEventMediaStore
) {
    private lateinit var photosContainer: LinearLayout
    private lateinit var emptyPhotosMessage: TextView
    private val itemFactory = EventMediaItemFactory(activity)

    private val takePhotoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let(::saveCameraPhoto) }
    private val selectPhotoLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::copyGalleryPhoto) }
    private val cameraPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) takePhotoLauncher.launch(null) }

    fun bind() {
        photosContainer = activity.findViewById(R.id.photosContainer)
        emptyPhotosMessage = activity.findViewById(R.id.tvEmptyPhotos)
        activity.findViewById<MaterialButton>(R.id.btnAddPhoto)
            .setOnClickListener { showPhotoOptions() }
        loadPhotos()
    }

    fun release() = Unit

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
        ) takePhotoLauncher.launch(null)
        else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun saveCameraPhoto(bitmap: Bitmap) {
        val photoFile = mediaStore.createFile("event_photos", "jpg")
        if (photoFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }) {
            addPhoto(photoFile)
        } else photoFile.delete()
    }

    private fun copyGalleryPhoto(sourceUri: android.net.Uri) {
        val photoFile = mediaStore.createFile("event_photos", "jpg")
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

    private fun addPhoto(photoFile: File) {
        val photos = mediaStore.readMedia(PHOTO_PREFERENCES, "photos")
        photos.add(0, newMedia(photoFile))
        mediaStore.saveMedia(PHOTO_PREFERENCES, "photos", photos)
        loadPhotos()
    }

    private fun loadPhotos() {
        photosContainer.removeAllViews()
        val photos = mediaStore.readMedia(PHOTO_PREFERENCES, "photos").filter { File(it.path).isFile }
        emptyPhotosMessage.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        photos.forEach(::addPhotoView)
    }

    private fun addPhotoView(photo: LocalEventMedia) {
        val item = itemFactory.item()
        item.addView(itemFactory.authorText(photo.authorName))
        item.addView(itemFactory.dateText(photo.createdAt))
        item.addView(ImageView(activity).apply {
            setImageURI(android.net.Uri.fromFile(File(photo.path)))
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Foto publicada no evento"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemFactory.dp(220))
                .apply { topMargin = itemFactory.dp(12) }
        })
        if (policy.canManagePublication(photo.authorId, userId)) {
            item.addView(itemFactory.deleteButton { deletePhoto(photo) })
        }
        photosContainer.addView(item)
    }

    private fun deletePhoto(photo: LocalEventMedia) {
        mediaStore.readMedia(PHOTO_PREFERENCES, "photos")
            .filterNot { it.id == photo.id }
            .also { mediaStore.saveMedia(PHOTO_PREFERENCES, "photos", it) }
        File(photo.path).delete()
        loadPhotos()
    }

    private fun newMedia(file: File) = LocalEventMedia(
        UUID.randomUUID().toString(), userId, authorName, System.currentTimeMillis(), file.absolutePath
    )

    private companion object { const val PHOTO_PREFERENCES = "event_photos" }
}
