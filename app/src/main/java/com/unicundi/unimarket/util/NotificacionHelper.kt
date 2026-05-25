package com.unicundi.unimarket.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificacionHelper {

    const val CANAL_ID = "unimarket_mensajes"
    private const val NOTIF_BASE_ID = 2000

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Mensajes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mensajes nuevos en UniMarket"
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(canal)
        }
    }

    fun mostrarNotificacion(context: Context, senderNombre: String, senderId: Long) {
        val abrirAppIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.toInt(),
            abrirAppIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("UniMarket")
            .setContentText("Tienes un mensaje nuevo de $senderNombre")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            // Una notificación por remitente; se actualiza si llegan más del mismo
            NotificationManagerCompat.from(context).notify(
                NOTIF_BASE_ID + senderId.toInt(),
                notif
            )
        } catch (_: SecurityException) { /* permiso no concedido */ }
    }
}
