package com.example.ecopotapp

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.*

data class WateringEvent(
    val plantId: Int,
    val moisture: Int,
    val timestamp: Long,
    val trigger: String
)

object HistoryManager {
    private val history = mutableListOf<WateringEvent>()

    fun addEvent(event: WateringEvent) {
        history.add(0, event)
        if (history.size > 200) history.removeAt(history.size - 1)
    }

    fun getAll(): List<WateringEvent> = history.toList()
    fun getForPlant(id: Int): List<WateringEvent> = history.filter { it.plantId == id }
    fun getTotalCount(): Int = history.size
    fun getCountToday(): Int {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        return history.count { it.timestamp >= startOfDay }
    }
}

class HistoryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var filterGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        container   = findViewById(R.id.historyContainer)
        filterGroup = findViewById(R.id.filterGroup)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val events = HistoryManager.getAll()
        findViewById<TextView>(R.id.totalCount).text  = events.size.toString()
        findViewById<TextView>(R.id.todayCount).text  = HistoryManager.getCountToday().toString()
        findViewById<TextView>(R.id.plant1Count).text = HistoryManager.getForPlant(1).size.toString()
        findViewById<TextView>(R.id.plant2Count).text = HistoryManager.getForPlant(2).size.toString()

        filterGroup.setOnCheckedChangeListener { _, checkedId ->
            val filter = when (checkedId) { R.id.filterP1 -> 1; R.id.filterP2 -> 2; else -> 0 }
            renderHistory(filter)
        }
        renderHistory(0)
    }

    private fun renderHistory(filter: Int) {
        container.removeAllViews()
        val events = when (filter) { 1 -> HistoryManager.getForPlant(1); 2 -> HistoryManager.getForPlant(2); else -> HistoryManager.getAll() }

        if (events.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "💧 No watering events yet."
                textSize = 14f; setTextColor(Color.parseColor("#8b949e"))
                setPadding(0, 48, 0, 0); gravity = android.view.Gravity.CENTER
            })
            return
        }

        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        events.forEach { event ->
            val card = CardView(this).apply {
                radius = 12f; cardElevation = 0f
                setCardBackgroundColor(Color.parseColor("#161b22"))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 10 }
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 18, 24, 18); gravity = android.view.Gravity.CENTER_VERTICAL
            }

            row.addView(TextView(this).apply {
                text = when (event.trigger) { "auto" -> "⚡"; "scheduled" -> "📅"; else -> "💧" }
                textSize = 20f; setPadding(0, 0, 16, 0)
            })

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            info.addView(TextView(this).apply { text = "Plant ${event.plantId}"; textSize = 14f; setTextColor(Color.parseColor("#e6edf3")) })
            info.addView(TextView(this).apply {
                text = "${event.trigger.replaceFirstChar { it.uppercase() }} • Moisture: ${event.moisture}%"
                textSize = 11f; setTextColor(Color.parseColor("#8b949e"))
            })
            row.addView(info)

            row.addView(TextView(this).apply {
                text = "●"; textSize = 12f; setPadding(0, 0, 10, 0)
                setTextColor(when { event.moisture < 20 -> Color.parseColor("#f85149"); event.moisture < 40 -> Color.parseColor("#d29922"); else -> Color.parseColor("#3fb950") })
            })
            row.addView(TextView(this).apply { text = sdf.format(Date(event.timestamp)); textSize = 11f; setTextColor(Color.parseColor("#8b949e")) })

            card.addView(row); container.addView(card)
        }
    }
}