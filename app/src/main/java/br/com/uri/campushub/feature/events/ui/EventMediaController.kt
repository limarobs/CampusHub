package br.com.uri.campushub.feature.events.ui

import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsPolicy

/** Coordinates the media controllers associated with one event. */
internal class EventMediaController(
    activity: AppCompatActivity,
    eventId: String,
    userId: String,
    authorName: String,
    policy: EventDetailsPolicy
) {
    private val mediaStore = LocalEventMediaStore(activity, eventId)
    private val photoController = EventPhotoController(
        activity, eventId, userId, authorName, policy, mediaStore
    )
    private val videoController = EventVideoController(
        activity, eventId, userId, authorName, policy, mediaStore
    )
    private val audioController = EventAudioController(
        activity, eventId, userId, authorName, policy, mediaStore
    )

    fun bind() {
        photoController.bind()
        videoController.bind()
        audioController.bind()
    }

    fun release() {
        photoController.release()
        videoController.release()
        audioController.release()
    }
}
