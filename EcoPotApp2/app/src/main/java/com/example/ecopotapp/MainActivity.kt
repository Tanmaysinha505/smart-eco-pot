package com.example.ecopotapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val ESP = "http://192.168.4.1"
    private val handler = Handler(Looper.getMainLooper())
    private var running = true

    private lateinit var badge: TextView
    private lateinit var nativeView: ScrollView
    private lateinit var webView: WebView
    private lateinit var tabDashboard: TextView
    private lateinit var tabBrowser: TextView
    private lateinit var alertBanner: TextView

    private lateinit var statOnlineVal: TextView
    private lateinit var statAvgVal: TextView
    private lateinit var statWaterVal: TextView

    private lateinit var moistureVal1: TextView
    private lateinit var statusLabel1: TextView
    private lateinit var bar1: ProgressBar
    private lateinit var btnWater1: Button
    private lateinit var btnAuto1: Button
    private lateinit var thr1: SeekBar
    private lateinit var thrLabel1: TextView

    private lateinit var moistureVal2: TextView
    private lateinit var statusLabel2: TextView
    private lateinit var bar2: ProgressBar
    private lateinit var btnWater2: Button
    private lateinit var btnAuto2: Button
    private lateinit var thr2: SeekBar
    private lateinit var thrLabel2: TextView

    private val autoMode  = mutableMapOf(1 to true,  2 to true)
    private val pumpState = mutableMapOf(1 to false, 2 to false)
    private var totalWaterings = 0
    private var consecutiveOfflineCount = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AlertManager.init(this)
        requestNotificationPermission()

        bindViews()
        setupTabs()
        setupWebView()
        setupButtons()
        setupBottomNav()
        startPolling()
        startScheduleChecker()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }

    private fun bindViews() {
        badge         = findViewById(R.id.connectionBadge)
        nativeView    = findViewById(R.id.nativeView)
        webView       = findViewById(R.id.webView)
        tabDashboard  = findViewById(R.id.tabDashboard)
        tabBrowser    = findViewById(R.id.tabBrowser)
        alertBanner   = findViewById(R.id.alertBanner)

        statOnlineVal = findViewById(R.id.statOnlineVal)
        statAvgVal    = findViewById(R.id.statAvgVal)
        statWaterVal  = findViewById(R.id.statWaterVal)

        moistureVal1  = findViewById(R.id.moistureValue1)
        statusLabel1  = findViewById(R.id.statusLabel1)
        bar1          = findViewById(R.id.moistureBar1)
        btnWater1     = findViewById(R.id.water1)
        btnAuto1      = findViewById(R.id.auto1)
        thr1          = findViewById(R.id.threshold1)
        thrLabel1     = findViewById(R.id.thrLabel1)

        moistureVal2  = findViewById(R.id.moistureValue2)
        statusLabel2  = findViewById(R.id.statusLabel2)
        bar2          = findViewById(R.id.moistureBar2)
        btnWater2     = findViewById(R.id.water2)
        btnAuto2      = findViewById(R.id.auto2)
        thr2          = findViewById(R.id.threshold2)
        thrLabel2     = findViewById(R.id.thrLabel2)
    }

    private fun setupTabs() {
        fun selectDashboard() {
            nativeView.visibility = View.VISIBLE
            webView.visibility    = View.GONE
            tabDashboard.setTextColor(Color.parseColor("#3fb950"))
            tabDashboard.setBackgroundColor(Color.parseColor("#1c2128"))
            tabBrowser.setTextColor(Color.parseColor("#8b949e"))
            tabBrowser.setBackgroundColor(Color.parseColor("#161b22"))
        }
        fun selectBrowser() {
            nativeView.visibility = View.GONE
            webView.visibility    = View.VISIBLE
            webView.loadUrl(ESP)
            tabBrowser.setTextColor(Color.parseColor("#3fb950"))
            tabBrowser.setBackgroundColor(Color.parseColor("#1c2128"))
            tabDashboard.setTextColor(Color.parseColor("#8b949e"))
            tabDashboard.setBackgroundColor(Color.parseColor("#161b22"))
        }
        tabDashboard.setOnClickListener { selectDashboard() }
        tabBrowser.setOnClickListener   { selectBrowser()   }
        selectDashboard()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                badge.text = "● Connected"
                badge.setTextColor(Color.parseColor("#3fb950"))
            }
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                badge.text = "○ Offline"
                badge.setTextColor(Color.parseColor("#f85149"))
            }
        }
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.navAnalytics).setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        findViewById<View>(R.id.navChatbot).setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
        findViewById<View>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.navSchedule).setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
    }

    private fun setupButtons() {
        btnWater1.setOnClickListener {
            val newState = if (pumpState[1] == true) 0 else 1
            httpGet("$ESP/pump?id=1&state=$newState")
            if (newState == 1) {
                HistoryManager.addEvent(WateringEvent(1, bar1.progress, System.currentTimeMillis(), "manual"))
                showAlertBanner("💧 Watering Plant 1...")
            } else {
                showAlertBanner("🛑 Plant 1 pump stopped.")
            }
        }

        btnWater2.setOnClickListener {
            val newState = if (pumpState[2] == true) 0 else 1
            httpGet("$ESP/pump?id=2&state=$newState")
            if (newState == 1) {
                HistoryManager.addEvent(WateringEvent(2, bar2.progress, System.currentTimeMillis(), "manual"))
                showAlertBanner("💧 Watering Plant 2...")
            } else {
                showAlertBanner("🛑 Plant 2 pump stopped.")
            }
        }

        btnAuto1.setOnClickListener {
            val newState = if (autoMode[1] == true) 0 else 1
            httpGet("$ESP/auto?id=1&state=$newState")
            autoMode[1] = (newState == 1)
            updateAutoBtn(btnAuto1, newState == 1)
            showAlertBanner(if (newState == 1) "⚡ Plant 1 Auto mode ON" else "✋ Plant 1 Manual mode")
        }

        btnAuto2.setOnClickListener {
            val newState = if (autoMode[2] == true) 0 else 1
            httpGet("$ESP/auto?id=2&state=$newState")
            autoMode[2] = (newState == 1)
            updateAutoBtn(btnAuto2, newState == 1)
            showAlertBanner(if (newState == 1) "⚡ Plant 2 Auto mode ON" else "✋ Plant 2 Manual mode")
        }

        thr1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, user: Boolean) {
                thrLabel1.text = "Threshold: $v%"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                httpGet("$ESP/threshold?id=1&val=${sb.progress}")
                showAlertBanner("Plant 1 threshold set to ${sb.progress}%")
            }
        })

        thr2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, user: Boolean) {
                thrLabel2.text = "Threshold: $v%"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                httpGet("$ESP/threshold?id=2&val=${sb.progress}")
                showAlertBanner("Plant 2 threshold set to ${sb.progress}%")
            }
        })
    }

    private fun showAlertBanner(msg: String) {
        alertBanner.text = msg
        alertBanner.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages("banner")
        handler.postDelayed({ alertBanner.visibility = View.GONE }, 3000)
    }

    private fun startScheduleChecker() {
        val checker = object : Runnable {
            override fun run() {
                if (!running) return
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val min  = cal.get(Calendar.MINUTE)
                val dow  = cal.get(Calendar.DAY_OF_WEEK) - 1

                listOf(1, 2).forEach { plantId ->
                    val sched = ScheduleManager.schedules[plantId] ?: return@forEach
                    if (sched.enabled && sched.hour == hour && sched.minute == min && dow in sched.days) {
                        httpGet("$ESP/pump?id=$plantId&state=1")
                        val moisture = if (plantId == 1) bar1.progress else bar2.progress
                        HistoryManager.addEvent(
                            WateringEvent(plantId, moisture, System.currentTimeMillis(), "scheduled")
                        )
                        handler.postDelayed({
                            httpGet("$ESP/pump?id=$plantId&state=0")
                        }, sched.durationSeconds * 1000L)
                        showAlertBanner("📅 Scheduled watering Plant $plantId!")
                    }
                }
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(checker)
    }

    private fun startPolling() {
        val poll = object : Runnable {
            override fun run() {
                if (!running) return
                Thread {
                    try {
                        val conn = java.net.URL("$ESP/status").openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 4000
                        conn.readTimeout    = 4000
                        val body = conn.inputStream.bufferedReader().readText()
                        val arr  = JSONArray(body)
                        consecutiveOfflineCount = 0

                        runOnUiThread {
                            badge.text = "● Live"
                            badge.setTextColor(Color.parseColor("#3fb950"))

                            var onlineCount   = 0
                            var moistureSum   = 0
                            var moistureCount = 0
                            var m1 = 0; var m2 = 0
                            totalWaterings = 0

                            for (i in 0 until arr.length()) {
                                val d = arr.getJSONObject(i)
                                updatePlant(d)
                                if (d.getBoolean("connected")) {
                                    onlineCount++
                                    val m = d.getInt("moisture")
                                    moistureSum += m
                                    moistureCount++
                                    if (d.getInt("id") == 1) m1 = m else m2 = m
                                    AlertManager.checkAndAlert(
                                        this@MainActivity, d.getInt("id"), m, d.getBoolean("pump")
                                    )
                                }
                                totalWaterings += d.getInt("waterCount")
                            }

                            MoistureHistory.add(m1, m2)
                            statOnlineVal.text = "$onlineCount/2"
                            statAvgVal.text    = if (moistureCount > 0) "${moistureSum / moistureCount}%" else "—"
                            statWaterVal.text  = "$totalWaterings"
                        }
                    } catch (e: Exception) {
                        consecutiveOfflineCount++
                        runOnUiThread {
                            badge.text = "○ Offline"
                            badge.setTextColor(Color.parseColor("#f85149"))
                            if (consecutiveOfflineCount >= 3) {
                                AlertManager.sendOfflineAlert(this@MainActivity)
                            }
                        }
                    }
                }.start()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(poll)
    }

    private fun updatePlant(d: org.json.JSONObject) {
        val id       = d.getInt("id")
        val moisture = d.getInt("moisture")
        val pump     = d.getBoolean("pump")
        val auto     = d.getBoolean("auto")
        val thr      = d.getInt("threshold")

        // Log auto watering events
        val wasOff = pumpState[id] == false
        if (pump && wasOff && auto) {
            HistoryManager.addEvent(WateringEvent(id, moisture, System.currentTimeMillis(), "auto"))
        }

        pumpState[id] = pump
        autoMode[id]  = auto

        val color = when {
            moisture < 20 -> Color.parseColor("#f85149")
            moisture < 40 -> Color.parseColor("#d29922")
            else           -> Color.parseColor("#3fb950")
        }
        val status = when {
            moisture < 20 -> "Very Dry ⚠️"
            moisture < 40 -> "Needs Water"
            moisture < 70 -> "Healthy ✅"
            else           -> "Well Watered 💧"
        }

        if (id == 1) {
            moistureVal1.text = "$moisture%"
            moistureVal1.setTextColor(color)
            statusLabel1.text = status
            bar1.progress     = moisture
            thrLabel1.text    = "Threshold: $thr%"
            if (thr1.progress != thr) thr1.progress = thr
            btnWater1.text    = if (pump) "🛑 Stop" else "💧 Water"
            updateAutoBtn(btnAuto1, auto)
        } else {
            moistureVal2.text = "$moisture%"
            moistureVal2.setTextColor(color)
            statusLabel2.text = status
            bar2.progress     = moisture
            thrLabel2.text    = "Threshold: $thr%"
            if (thr2.progress != thr) thr2.progress = thr
            btnWater2.text    = if (pump) "🛑 Stop" else "💧 Water"
            updateAutoBtn(btnAuto2, auto)
        }
    }

    private fun updateAutoBtn(btn: Button, isAuto: Boolean) {
        btn.text = if (isAuto) "⚡ Auto ON" else "✋ Manual"
    }

    private fun httpGet(url: String) {
        Thread {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout    = 3000
                conn.inputStream.close()
            } catch (_: Exception) {}
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}