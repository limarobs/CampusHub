package br.com.uri.campushub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class EventDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_event_details)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvEventDetailsTitle).text =
            intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<TextView>(R.id.tvEventDetailsDescription).text =
            intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        findViewById<TextView>(R.id.tvEventDetailsDate).text =
            intent.getStringExtra(EXTRA_DATE).orEmpty()
        findViewById<TextView>(R.id.tvEventDetailsLocation).text =
            intent.getStringExtra(EXTRA_LOCATION).orEmpty()
    }

    private fun navigateToLogin() {
        val loginIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish()
    }

    companion object {
        private const val EXTRA_EVENT_ID = "extra_event_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_DESCRIPTION = "extra_description"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_LOCATION = "extra_location"

        fun createIntent(context: Context, event: CampusEvent): Intent {
            return Intent(context, EventDetailsActivity::class.java).apply {
                putExtra(EXTRA_EVENT_ID, event.id)
                putExtra(EXTRA_TITLE, event.title)
                putExtra(EXTRA_DESCRIPTION, event.description)
                putExtra(EXTRA_DATE, event.date)
                putExtra(EXTRA_LOCATION, event.location)
            }
        }
    }
}
