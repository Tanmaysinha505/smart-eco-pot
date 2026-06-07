package com.example.ecopotapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class ChatbotActivity : AppCompatActivity() {

    private val ESP = "http://192.168.4.1"
    private val CLAUDE_API = "https://api.anthropic.com/v1/messages"
    private val API_KEY = "YOUR_ANTHROPIC_API_KEY_HERE"

    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var inputField: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var typingIndicator: TextView
    private lateinit var connectionChip: TextView

    private val messages = mutableListOf<ChatMessage>()
    private var plantData: JSONArray? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var fetchRunnable: Runnable? = null

    // Typing dots animation
    private var dotCount = 0
    private val dotRunnable = object : Runnable {
        override fun run() {
            if (typingIndicator.visibility == View.VISIBLE) {
                dotCount = (dotCount + 1) % 4
                typingIndicator.text = "EcoBot is thinking" + ".".repeat(dotCount)
                handler.postDelayed(this, 400)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        chatContainer   = findViewById(R.id.chatContainer)
        scrollView      = findViewById(R.id.chatScrollView)
        inputField      = findViewById(R.id.messageInput)
        sendBtn         = findViewById(R.id.sendButton)
        typingIndicator = findViewById(R.id.typingIndicator)
        connectionChip  = findViewById(R.id.connectionChip)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        startFetchingPlantStatus()
        addBotMessage("🌿 Hi! I'm EcoBot, your plant assistant.\n\nI can:\n• Check plant status\n• Control pumps & auto mode\n• Answer plant care questions\n• Give watering tips\n\nTry tapping a quick action below!")

        sendBtn.setOnClickListener { sendMessage() }
        inputField.setOnEditorActionListener { _, _, _ -> sendMessage(); true }

        setupQuickActions()
    }

    private fun setupQuickActions() {
        val quickActions = listOf(
            "🌡 Status" to "Plant Status",
            "💧 Water All" to "Water All",
            "🛑 Stop All" to "Stop All",
            "🌱 Care Tips" to "Care Tips",
            "📊 Analytics" to "Analytics Info"
        )
        val chipRow = findViewById<LinearLayout>(R.id.quickActionRow)
        chipRow.removeAllViews()

        quickActions.forEach { (label, command) ->
            val chip = TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(Color.parseColor("#3fb950"))
                setPadding(20, 10, 20, 10)
                background = getDrawable(R.drawable.chip_outline)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                setOnClickListener {
                    inputField.setText(command)
                    sendMessage()
                }
            }
            chipRow.addView(chip)
        }
    }

    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return
        inputField.setText("")
        addUserMessage(text)
        showTyping(true)

        val ruleResponse = handleRuleBasedCommand(text)
        if (ruleResponse != null) {
            handler.postDelayed({
                showTyping(false)
                addBotMessage(ruleResponse)
            }, 500)
        } else {
            callClaudeAPI(text)
        }
    }

    private fun handleRuleBasedCommand(input: String): String? {
        val lower = input.lowercase().trim()
        return when {
            // Water commands
            lower.contains("water all") || lower.contains("water both") -> {
                httpGet("$ESP/pump?id=1&state=1")
                httpGet("$ESP/pump?id=2&state=1")
                "💧 Watering both plants now!\n\nRemember to stop manually or let Auto mode handle it once moisture rises above threshold."
            }
            lower.contains("stop all") || lower.contains("stop both") -> {
                httpGet("$ESP/pump?id=1&state=0")
                httpGet("$ESP/pump?id=2&state=0")
                "🛑 All pumps stopped."
            }
            (lower.contains("water") || lower.contains("pump on")) && lower.contains("1") && !lower.contains("2") -> {
                httpGet("$ESP/pump?id=1&state=1")
                val m = getMoisture(1)
                "💧 Watering Plant 1!\n${if (m != null) "Current moisture: $m%" else ""}"
            }
            (lower.contains("water") || lower.contains("pump on")) && lower.contains("2") && !lower.contains("1") -> {
                httpGet("$ESP/pump?id=2&state=1")
                val m = getMoisture(2)
                "💧 Watering Plant 2!\n${if (m != null) "Current moisture: $m%" else ""}"
            }
            lower.contains("stop") && lower.contains("1") && !lower.contains("2") -> {
                httpGet("$ESP/pump?id=1&state=0"); "🛑 Plant 1 pump stopped."
            }
            lower.contains("stop") && lower.contains("2") && !lower.contains("1") -> {
                httpGet("$ESP/pump?id=2&state=0"); "🛑 Plant 2 pump stopped."
            }
            // Auto mode commands
            (lower.contains("auto on") || lower.contains("enable auto")) && lower.contains("1") -> {
                httpGet("$ESP/auto?id=1&state=1"); "⚡ Auto mode ON for Plant 1.\nPump will trigger automatically based on moisture threshold."
            }
            (lower.contains("auto off") || lower.contains("disable auto") || lower.contains("manual")) && lower.contains("1") -> {
                httpGet("$ESP/auto?id=1&state=0"); "✋ Plant 1 switched to Manual mode."
            }
            (lower.contains("auto on") || lower.contains("enable auto")) && lower.contains("2") -> {
                httpGet("$ESP/auto?id=2&state=1"); "⚡ Auto mode ON for Plant 2.\nPump will trigger automatically based on moisture threshold."
            }
            (lower.contains("auto off") || lower.contains("disable auto") || lower.contains("manual")) && lower.contains("2") -> {
                httpGet("$ESP/auto?id=2&state=0"); "✋ Plant 2 switched to Manual mode."
            }
            // Status
            lower.contains("status") || lower.contains("plant status") || lower.contains("how are") -> {
                plantData?.let { buildStatusMessage(it) }
                    ?: if (!isConnected) "📡 ESP32 is offline. Connect your phone to EcoPot-Garden WiFi."
                    else "🔄 Fetching live data, try again in a moment."
            }
            // Tips
            lower.contains("care tips") || lower.contains("tip") || lower.contains("advice") -> {
                "🌱 Plant Care Tips:\n\n• Most plants thrive at 40–60% moisture\n• Water deeply but infrequently\n• Morning watering reduces fungal risk\n• Yellow leaves → overwatering\n• Brown crispy tips → underwatering\n• Use Auto mode to avoid manual monitoring\n• Check drainage if moisture stays very high"
            }
            // Analytics info
            lower.contains("analytics") || lower.contains("chart") || lower.contains("graph") -> {
                "📊 Analytics are available in the Analytics tab on the main screen!\n\nYou can view moisture trends for both plants over time and toggle each plant line on/off."
            }
            // Schedule info
            lower.contains("schedule") || lower.contains("timer") || lower.contains("automatic time") -> {
                "📅 Use the Schedule tab to:\n• Set daily watering times per plant\n• Choose days of the week\n• Set watering duration\n\nScheduled waterings are tracked in History too!"
            }
            // Connection help
            lower.contains("offline") || lower.contains("not connect") || lower.contains("can't connect") -> {
                "📡 Connection Tips:\n\n1. Connect phone to 'EcoPot-Garden' WiFi\n2. Password: water1234\n3. Make sure ESP32 is powered on\n4. Open app — status turns green when live\n\nThe app talks directly to your ESP32 at 192.168.4.1"
            }
            else -> null  // Send to Claude AI
        }
    }

    private fun getMoisture(plantId: Int): Int? {
        return try {
            plantData?.let {
                for (i in 0 until it.length()) {
                    val d = it.getJSONObject(i)
                    if (d.getInt("id") == plantId) return d.getInt("moisture")
                }
                null
            }
        } catch (e: Exception) { null }
    }

    private fun buildStatusMessage(data: JSONArray): String {
        val sb = StringBuilder("📊 Live Plant Status:\n\n")
        for (i in 0 until data.length()) {
            val d = data.getJSONObject(i)
            val id        = d.getInt("id")
            val moisture  = d.getInt("moisture")
            val pump      = d.getBoolean("pump")
            val auto      = d.getBoolean("auto")
            val connected = d.getBoolean("connected")
            val threshold = d.optInt("threshold", 30)

            val emoji = if (!connected) "❌" else when {
                moisture < 20 -> "🔴"; moisture < 40 -> "🟡"; else -> "🟢"
            }
            val status = when {
                !connected   -> "Offline"
                moisture < 20 -> "Very Dry ⚠️"
                moisture < 40 -> "Needs Water"
                moisture < 70 -> "Healthy"
                else          -> "Well Watered"
            }
            sb.append("$emoji Plant $id\n")
            sb.append("   Moisture: $moisture% — $status\n")
            sb.append("   Pump: ${if (pump) "ON 💧" else "OFF"} | Mode: ${if (auto) "Auto ⚡" else "Manual ✋"}\n")
            sb.append("   Threshold: $threshold%\n\n")
        }
        sb.append("Last updated: just now")
        return sb.toString().trimEnd()
    }

    private fun callClaudeAPI(userMessage: String) {
        Thread {
            try {
                val plantContext = plantData?.let { buildStatusMessage(it) }
                    ?: "Plant data currently unavailable (ESP32 may be offline)"

                val systemPrompt = """You are EcoBot, a friendly and knowledgeable plant care assistant built into the EcoPot smart garden app.

Current plant sensor data:
$plantContext

Your capabilities:
- Answer plant care and gardening questions
- Explain moisture levels and what they mean for plants
- Suggest watering schedules and care routines
- Help troubleshoot plant problems

Rules:
- Keep responses concise and practical (under 150 words)
- Use emojis sparingly but helpfully
- If asked to control pumps, remind users to use the quick-action buttons or dashboard
- If plant data shows a problem, proactively mention it
- Be warm and encouraging about plant care"""

                val historyArray = JSONArray()
                messages.takeLast(8).forEach { msg ->
                    historyArray.put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "assistant")
                        put("content", msg.text)
                    })
                }
                historyArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })

                val body = JSONObject().apply {
                    put("model", "claude-sonnet-4-20250514")
                    put("max_tokens", 400)
                    put("system", systemPrompt)
                    put("messages", historyArray)
                }

                val conn = URL(CLAUDE_API).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-api-key", API_KEY)
                conn.setRequestProperty("anthropic-version", "2023-06-01")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val reply = JSONObject(response)
                        .getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text")
                    runOnUiThread { showTyping(false); addBotMessage(reply) }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    runOnUiThread {
                        showTyping(false)
                        addBotMessage("⚠️ AI service error ($responseCode). Your plant commands still work via quick actions!")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showTyping(false)
                    addBotMessage("⚠️ Couldn't reach AI service. Check your internet connection.\n\nPlant control commands still work — try the quick action chips!")
                }
            }
        }.start()
    }

    private fun addUserMessage(text: String) {
        val msg = ChatMessage(text, true)
        messages.add(msg)
        renderMessage(msg)
    }

    private fun addBotMessage(text: String) {
        val msg = ChatMessage(text, false)
        messages.add(msg)
        renderMessage(msg)
    }

    @SuppressLint("SetTextI18n")
    private fun renderMessage(msg: ChatMessage) {
        val outerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4; bottomMargin = 4 }
            gravity = if (msg.isUser) Gravity.END else Gravity.START
        }

        val card = CardView(this).apply {
            radius = 20f
            cardElevation = 2f
            setCardBackgroundColor(
                if (msg.isUser) Color.parseColor("#1f6feb")
                else Color.parseColor("#1c2128")
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (msg.isUser) marginStart = 80 else marginEnd = 80
            }
        }

        val tv = TextView(this).apply {
            text = msg.text
            textSize = 14f
            setTextColor(Color.parseColor("#e6edf3"))
            setPadding(28, 16, 28, 16)
            setLineSpacing(4f, 1f)
        }

        card.addView(tv)
        outerLayout.addView(card)
        chatContainer.addView(outerLayout)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun showTyping(show: Boolean) {
        typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            dotCount = 0
            handler.post(dotRunnable)
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        } else {
            handler.removeCallbacks(dotRunnable)
        }
    }

    private fun startFetchingPlantStatus() {
        fetchRunnable = object : Runnable {
            override fun run() {
                Thread {
                    try {
                        val conn = URL("$ESP/status").openConnection() as HttpURLConnection
                        conn.connectTimeout = 4000
                        conn.readTimeout = 4000
                        val text = conn.inputStream.bufferedReader().readText()
                        plantData = JSONArray(text)
                        isConnected = true
                        runOnUiThread {
                            connectionChip.text = "● Live"
                            connectionChip.setTextColor(Color.parseColor("#3fb950"))
                        }
                    } catch (e: Exception) {
                        isConnected = false
                        runOnUiThread {
                            connectionChip.text = "○ Offline"
                            connectionChip.setTextColor(Color.parseColor("#f85149"))
                        }
                    }
                    handler.postDelayed(fetchRunnable!!, 8000)
                }.start()
            }
        }
        handler.post(fetchRunnable!!)
    }

    private fun httpGet(url: String) {
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.inputStream.close()
            } catch (_: Exception) {}
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacks(dotRunnable)
        handler.removeCallbacksAndMessages(null)
    }
}