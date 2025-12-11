package com.urasweb.aqualife

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Si en algún momento quieres respetar "aguaEnabled" desde prefs, puedes hacer:
        // val prefs = context.getSharedPreferences("imc_prefs", Context.MODE_PRIVATE)
        // if (!prefs.getBoolean("reminder_agua_enabled", true)) return

        val mensaje = "Oye, tu cuerpo pide agua 🎵"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "agua_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de agua",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_aqualife_icon) // icono pequeño de tu app
            .setContentTitle("AQuaLife")
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setAutoCancel(true)

        // Usar un ID pseudo-único para permitir múltiples notifs
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
