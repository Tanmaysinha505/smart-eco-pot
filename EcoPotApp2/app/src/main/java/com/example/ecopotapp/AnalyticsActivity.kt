package com.example.ecopotapp

import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

// ================= DATA STORAGE =================
object MoistureHistory {
    data class DataPoint(val timestamp: Long, val moisture1: Int, val moisture2: Int)

    private val points = mutableListOf<DataPoint>()

    fun add(m1: Int, m2: Int) {
        points.add(DataPoint(System.currentTimeMillis(), m1, m2))
        if (points.size > 120) points.removeAt(0)
    }

    fun getPoints(): List<DataPoint> = points.toList()
    fun getLast(n: Int): List<DataPoint> = points.takeLast(n)
}

// ================= CHART VIEW =================
class MoistureChartView(context: android.content.Context) : View(context) {

    private var data: List<MoistureHistory.DataPoint> = emptyList()
    private var showP1 = true
    private var showP2 = true

    private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3fb950")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#58a6ff")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#21262d")
        strokeWidth = 1f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8b949e")
        textSize = 26f
    }

    fun setData(points: List<MoistureHistory.DataPoint>, p1: Boolean, p2: Boolean) {
        data = points
        showP1 = p1
        showP2 = p2
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val paddingLeft = 60f
        val paddingTop = 20f
        val chartWidth = w - paddingLeft - 20f
        val chartHeight = h - paddingTop - 40f

        canvas.drawColor(Color.parseColor("#0d1117"))

        if (data.isEmpty()) {
            canvas.drawText("Collecting data...", w / 3, h / 2, labelPaint)
            return
        }

        fun toX(i: Int) = paddingLeft + i.toFloat() / (data.size - 1).coerceAtLeast(1) * chartWidth
        fun toY(v: Int) = paddingTop + chartHeight * (1f - v / 100f)

        fun drawLine(getVal: (MoistureHistory.DataPoint) -> Int, paint: Paint) {
            val path = Path()
            data.forEachIndexed { i, pt ->
                val x = toX(i)
                val y = toY(getVal(pt))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
        }

        if (showP1) drawLine({ it.moisture1 }, paint1)
        if (showP2) drawLine({ it.moisture2 }, paint2)
    }
}

// ================= MAIN ACTIVITY =================
class AnalyticsActivity : AppCompatActivity() {

    private val ESP = "http://192.168.4.1"
    private val handler = Handler(Looper.getMainLooper())
    private var running = true

    private lateinit var chartView: MoistureChartView
    private var showP1 = true
    private var showP2 = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener { finish() }

        // ================= SAFE VIEW HANDLING =================
        val container = findViewById<FrameLayout>(R.id.chartContainer)
        if (container != null) {
            chartView = MoistureChartView(this)
            container.addView(chartView)
        }

        val btnP1 = findViewById<Button>(R.id.toggleP1)
        val btnP2 = findViewById<Button>(R.id.toggleP2)

        btnP1?.setOnClickListener {
            showP1 = !showP1
            btnP1.alpha = if (showP1) 1f else 0.4f
            updateChart()
        }

        btnP2?.setOnClickListener {
            showP2 = !showP2
            btnP2.alpha = if (showP2) 1f else 0.4f
            updateChart()
        }

        startPolling()
    }

    private fun startPolling() {
        val poll = object : Runnable {
            override fun run() {
                if (!running) return

                Thread {
                    try {
                        val conn = URL("$ESP/status").openConnection() as HttpURLConnection
                        val arr = JSONArray(conn.inputStream.bufferedReader().readText())

                        var m1 = 0
                        var m2 = 0

                        for (i in 0 until arr.length()) {
                            val d = arr.getJSONObject(i)
                            if (d.getInt("id") == 1) m1 = d.getInt("moisture")
                            else m2 = d.getInt("moisture")
                        }

                        MoistureHistory.add(m1, m2)

                        runOnUiThread {
                            updateChart()
                        }

                    } catch (_: Exception) {}

                }.start()

                handler.postDelayed(this, 3000)
            }
        }

        handler.post(poll)
    }

    private fun updateChart() {
        if (::chartView.isInitialized) {
            chartView.setData(MoistureHistory.getLast(60), showP1, showP2)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}