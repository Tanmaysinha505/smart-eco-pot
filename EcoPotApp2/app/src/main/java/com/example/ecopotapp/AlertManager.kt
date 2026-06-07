package com.example.ecopotapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object AlertManager {
    private const val CHANNEL_ID = "ecopot_alerts"
    private var notifId = 100
    private val lastAlertTime = mutableMapOf<String, Long>()
    private const val COOLDOWN = 5 * 60 * 1000L

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Plant Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "EcoPot plant alerts"; enableLights(true); enableVibration(true)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    fun checkAndAlert(context: Context, plantId: Int, moisture: Int, pump: Boolean) {
        val now = System.currentTimeMillis()
        when {
            moisture < 15 -> alertIf("crit_$plantId", now, context, "🔴 Plant $plantId Critical!", "Moisture at $moisture% — water immediately!", NotificationCompat.PRIORITY_MAX)
            moisture < 25 -> alertIf("dry_$plantId",  now, context, "🟡 Plant $plantId Needs Water", "Moisture dropped to $moisture%.", NotificationCompat.PRIORITY_DEFAULT)
            moisture > 85 -> alertIf("wet_$plantId",  now, context, "💧 Plant $plantId Overwatered?", "Moisture very high at $moisture%. Check drainage.", NotificationCompat.PRIORITY_LOW)
        }
    }

    fun sendOfflineAlert(context: Context) {
        alertIf("offline", System.currentTimeMillis(), context, "📡 EcoPot Offline", "Can't reach ESP32. Check WiFi.", NotificationCompat.PRIORITY_LOW, cooldown = 10 * 60 * 1000L)
    }

    private fun alertIf(key: String, now: Long, context: Context, title: String, body: String, priority: Int, cooldown: Long = COOLDOWN) {
        if (now - (lastAlertTime[key] ?: 0L) > cooldown) {
            lastAlertTime[key] = now
            val pi = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority).setContentIntent(pi).setAutoCancel(true).build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(notifId++, notif)
        }
    }
}