package com.unicundi.unimarket.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unicundi.unimarket.data.network.RetrofitClient
import com.unicundi.unimarket.util.NotificacionHelper

class MensajeCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val myId = prefs.getLong("usuarioId", 0L)
        if (myId == 0L) return Result.success()

        val ultimaId = prefs.getLong("ultima_msg_id", -1L)

        return try {
            val r = RetrofitClient.apiService.getMensajes()
            if (!r.isSuccessful) return Result.success()

            val mensajes = r.body() ?: emptyList()
            val recibidos = mensajes.filter { it.receiverId == myId }

            // Primera vez: solo marcar el máximo actual como visto, sin notificar
            if (ultimaId == -1L) {
                val maxId = recibidos.maxOfOrNull { it.id } ?: 0L
                prefs.edit().putLong("ultima_msg_id", maxId).apply()
                return Result.success()
            }

            val nuevos = recibidos.filter {
                it.id > ultimaId && !it.message.contains("__xpin_prod__")
            }

            if (nuevos.isNotEmpty()) {
                val maxId = nuevos.maxOf { it.id }
                prefs.edit()
                    .putLong("ultima_msg_id", maxId)
                    .putInt("msgs_no_leidos", prefs.getInt("msgs_no_leidos", 0) + nuevos.size)
                    .apply()

                nuevos.map { it.senderId to it.senderNombre }.distinctBy { it.first }
                    .forEach { (senderId, nombre) ->
                        NotificacionHelper.mostrarNotificacion(applicationContext, nombre, senderId)
                    }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
