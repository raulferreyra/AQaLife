package com.urasweb.aqualife

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

        val aguaEnabled = prefs.getBoolean("aguaEnabled", true)
        if (!aguaEnabled) return

        val horaInicio = prefs.getInt("horaInicio", 9)
        val minutoInicio = prefs.getInt("minutoInicio", 0)
        val horaFin = prefs.getInt("horaFin", 22)
        val minutoFin = prefs.getInt("minutoFin", 0)

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = horaInicio * 60 + minutoInicio
        val endMinutes = horaFin * 60 + minutoFin

        // Si estamos fuera del rango horario, no mostramos notificación
        if (nowMinutes < startMinutes || nowMinutes > endMinutes) {
            return
        }

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
            .setSmallIcon(R.drawable.ic_aqualife_icon)
            .setContentTitle("AQuaLife")
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
