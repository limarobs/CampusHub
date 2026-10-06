package br.com.uri.campushub.feature.events.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.model.CampusEvent
import br.com.uri.campushub.feature.events.viewmodel.EventStatusFilter
import br.com.uri.campushub.feature.events.viewmodel.EventsUiState
import br.com.uri.campushub.feature.events.viewmodel.EventsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class EventsActivity : AppCompatActivity() {

    private val viewModel: EventsViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var eventsScrollView: View
    private lateinit var eventsContainer: LinearLayout
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyDescription: TextView
    private lateinit var errorMessage: TextView
    private lateinit var retryButton: MaterialButton
    private lateinit var searchField: TextInputEditText
    private lateinit var categoryField: MaterialAutoCompleteTextView
    private lateinit var categoryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_events)
        bindViews()
        setListeners()

        viewModel.uiState.observe(this, ::renderState)
        viewModel.loadEvents()
    }

    private fun bindViews() {
        progressBar = findViewById(R.id.progressEvents)
        eventsScrollView = findViewById(R.id.eventsScrollView)
        eventsContainer = findViewById(R.id.eventsContainer)
        emptyState = findViewById(R.id.emptyState)
        emptyTitle = findViewById(R.id.tvEmptyEventsTitle)
        emptyDescription = findViewById(R.id.tvEmptyEventsDescription)
        errorMessage = findViewById(R.id.tvEventsError)
        retryButton = findViewById(R.id.btnRetryEvents)
        searchField = findViewById(R.id.etSearchEvents)
        categoryField = findViewById(R.id.actCategory)
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        categoryField.setAdapter(categoryAdapter)
    }

    private fun setListeners() {
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        retryButton.setOnClickListener { viewModel.loadEvents() }
        findViewById<Chip>(R.id.chipAllEvents).setOnClickListener {
            viewModel.updateStatus(EventStatusFilter.ALL)
        }
        findViewById<Chip>(R.id.chipUpcomingEvents).setOnClickListener {
            viewModel.updateStatus(EventStatusFilter.UPCOMING)
        }
        findViewById<Chip>(R.id.chipEndedEvents).setOnClickListener {
            viewModel.updateStatus(EventStatusFilter.ENDED)
        }
        categoryField.setOnItemClickListener { _, _, position, _ ->
            categoryAdapter.getItem(position)?.let(viewModel::updateCategory)
        }
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                viewModel.updateSearchTerm(s?.toString().orEmpty())
            }
        })
    }

    private fun renderState(state: EventsUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        errorMessage.visibility = if (state.hasError) View.VISIBLE else View.GONE
        retryButton.visibility = if (state.hasError) View.VISIBLE else View.GONE

        categoryAdapter.clear()
        categoryAdapter.addAll(state.categories)
        categoryAdapter.notifyDataSetChanged()
        if (categoryField.text.toString() != state.selectedCategory) {
            categoryField.setText(state.selectedCategory, false)
        }

        if (state.isLoading || state.hasError) {
            eventsScrollView.visibility = View.GONE
            emptyState.visibility = View.GONE
            return
        }

        eventsContainer.removeAllViews()
        if (state.events.isEmpty()) {
            emptyTitle.text = if (state.hasEvents) {
                "Nenhum evento encontrado"
            } else {
                "Nenhum evento disponível"
            }
            emptyDescription.text = if (state.hasEvents) {
                "Tente mudar a busca ou os filtros selecionados."
            } else {
                "Quando novos eventos forem publicados, eles aparecerão aqui."
            }
            emptyState.visibility = View.VISIBLE
            eventsScrollView.visibility = View.GONE
            return
        }

        state.events.forEach(::addEventCard)
        emptyState.visibility = View.GONE
        eventsScrollView.visibility = View.VISIBLE
    }

    private fun addEventCard(event: CampusEvent) {
        val card = layoutInflater.inflate(R.layout.item_event, eventsContainer, false)
        card.findViewById<TextView>(R.id.tvEventTitle).text = event.title
        card.findViewById<TextView>(R.id.tvEventDescription).text = event.description
        card.findViewById<TextView>(R.id.tvEventCategoryStatus).text =
            "${event.category} • ${if (isEventEnded(event)) "Encerrado" else "Próximo"}"
        card.findViewById<TextView>(R.id.tvEventDate).text = formatEventDate(event)
        card.findViewById<TextView>(R.id.tvEventLocation).text = event.location
        card.setOnClickListener {
            startActivity(EventDetailsActivity.createIntent(this, event))
        }
        eventsContainer.addView(card)
    }

    private fun isEventEnded(event: CampusEvent): Boolean {
        return event.endsAt?.toDate()?.time?.let { it <= System.currentTimeMillis() } == true
    }

    private fun formatEventDate(event: CampusEvent): String {
        if (event.date.isNotBlank()) {
            return event.date
        }

        val startDate = event.startsAt?.toDate() ?: return "Data a definir"
        return SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
            .format(startDate)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
