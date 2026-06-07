package com.example.ecopotapp

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

data class WateringSchedule(
    val plantId: Int,
    var enabled: Boolean,
    var hour: Int,
    var minute: Int,
    var durationSeconds: Int,
    var days: MutableSet<Int> = mutableSetOf(0,1,2,3,4,5,6)
)

object ScheduleManager {
    val schedules = mutableMapOf(
        1 to WateringSchedule(1, false, 8, 0, 10),
        2 to WateringSchedule(2, false, 8, 30, 10)
    )
}

class ScheduleActivity : AppCompatActivity() {

    private val ESP = "http://192.168.4.1"
    private val DAY_LABELS = listOf("Su","Mo","Tu","We","Th","Fr","Sa")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        setupPlantSchedule(1)
        setupPlantSchedule(2)
    }

    private fun setupPlantSchedule(plantId: Int) {
        val s = plantId.toString()
        val schedule = ScheduleManager.schedules[plantId]!!

        fun <T : android.view.View> fid(name: String): T = findViewById(resources.getIdentifier("$name$s", "id", packageName))

        val enableToggle  = fid<Switch>("scheduleEnabled")
        val timePicker    = fid<TimePicker>("timePicker")
        val durationSlider = fid<SeekBar>("durationSlider")
        val durationLabel = fid<TextView>("durationLabel")
        val dayContainer  = fid<LinearLayout>("dayContainer")
        val saveBtn       = fid<Button>("saveSchedule")
        val statusText    = fid<TextView>("scheduleStatus")

        enableToggle.isChecked = schedule.enabled
        timePicker.setIs24HourView(true)
        timePicker.hour = schedule.hour; timePicker.minute = schedule.minute
        durationSlider.max = 55; durationSlider.progress = schedule.durationSeconds - 5
        durationLabel.text = "Duration: ${schedule.durationSeconds}s"

        DAY_LABELS.forEachIndexed { idx, label ->
            dayContainer.addView(TextView(this).apply {
                text = label; textSize = 12f; gravity = android.view.Gravity.CENTER
                setPadding(12, 8, 12, 8)
                background = if (idx in schedule.days) getDrawable(R.drawable.btn_blue) else getDrawable(R.drawable.chip_outline)
                setTextColor(Color.parseColor(if (idx in schedule.days) "#ffffff" else "#8b949e"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { if (idx < 6) marginEnd = 4 }
                setOnClickListener {
                    if (idx in schedule.days) {
                        schedule.days.remove(idx)
                        background = getDrawable(R.drawable.chip_outline); setTextColor(Color.parseColor("#8b949e"))
                    } else {
                        schedule.days.add(idx)
                        background = getDrawable(R.drawable.btn_blue); setTextColor(Color.parseColor("#ffffff"))
                    }
                }
            })
        }

        durationSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, u: Boolean) { schedule.durationSeconds = v + 5; durationLabel.text = "Duration: ${v+5}s" }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        enableToggle.setOnCheckedChangeListener { _, checked ->
            schedule.enabled = checked; timePicker.isEnabled = checked; durationSlider.isEnabled = checked
            dayContainer.alpha = if (checked) 1f else 0.5f
        }
        timePicker.isEnabled = schedule.enabled; durationSlider.isEnabled = schedule.enabled
        dayContainer.alpha = if (schedule.enabled) 1f else 0.5f

        saveBtn.setOnClickListener {
            schedule.hour = timePicker.hour; schedule.minute = timePicker.minute; schedule.enabled = enableToggle.isChecked
            val url = "$ESP/schedule?id=$plantId&enabled=${if(schedule.enabled)1 else 0}&hour=${schedule.hour}&minute=${schedule.minute}&duration=${schedule.durationSeconds}&days=${schedule.days.sorted().joinToString(",")}"
            Thread {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000; conn.readTimeout = 3000; conn.inputStream.close()
                    runOnUiThread { statusText.setTextColor(Color.parseColor("#3fb950")); statusText.text = "✅ Saved! Next: ${String.format("%02d:%02d", schedule.hour, schedule.minute)}" }
                } catch (e: Exception) {
                    runOnUiThread { statusText.setTextColor(Color.parseColor("#d29922")); statusText.text = "⚠️ Saved locally — ESP32 offline." }
                }
            }.start()
        }
    }
}