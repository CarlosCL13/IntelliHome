package com.intelliworks.intellihome.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.intelliworks.intellihome.LightControlActivity
import com.intelliworks.intellihome.R

/**
 * Servicio de mensajería en segundo plano para IntelliHome.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        private const val CHANNEL_ID = "canal_emergencias_intelli_v4"
        private const val ACTION_EVENTO_IOT = "EVENTO_EMERGENCIA_IOT"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "IntelliHome:AlertaCriticaProcessor"
        )
        wakeLock.acquire(30000)

        try {
            Log.d(TAG, "Procesando mensaje entrante: ${remoteMessage.data}")

            val data = remoteMessage.data
            var tipoEvento = data["tipo"] ?: data["evento"] ?: ""
            val titulo = data["titulo"] ?: "Alerta de Seguridad"
            val cuerpo = data["cuerpo"] ?: "Evento crítico detectado en su propiedad."

            if (tipoEvento.isEmpty()) {
                if (titulo.contains("INCENDIO", true)) tipoEvento = "incendio"
                if (titulo.contains("SISMO", true)) tipoEvento = "sismo"
            }

            if (tipoEvento.isNotEmpty()) {
                persistirEstadoEmergencia(tipoEvento)
                ejecutarVibracionAlarma() // <--- AHORA ESTA FUNCIÓN ES ROBUSTA
                notificarUIEnTiempoReal(tipoEvento)
            }

            construirYMostrarNotificacion(titulo, cuerpo, tipoEvento)

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje FCM", e)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun persistirEstadoEmergencia(evento: String) {
        val prefs = getSharedPreferences("IntelliHome_Emergencia", Context.MODE_PRIVATE)
        prefs.edit().putString("evento_activo", evento).apply()
    }

    /**
     * CORREGIDO: Ahora incluye AudioAttributes con USAGE_ALARM.
     * Esto permite que la vibración sea agresiva y cancelable.
     */
    private fun ejecutarVibracionAlarma() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                // Patrón agresivo (aprox 20-30 segs)
                val ciclos = 10
                val tiempos = LongArray(ciclos * 2) { i -> if (i % 2 == 0) 500L else 2000L }
                tiempos[0] = 0
                val amplitudes = IntArray(ciclos * 2) { i -> if (i % 2 == 0) 0 else 255 }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // AQUÍ ESTÁ EL CAMBIO CLAVE:
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM) // Se marca como ALARMA
                        .build()

                    vibrator.vibrate(
                        VibrationEffect.createWaveform(tiempos, amplitudes, -1),
                        audioAttributes // Se pasan los atributos
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(tiempos, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al acceder al motor de vibración", e)
        }
    }

    private fun notificarUIEnTiempoReal(evento: String) {
        val intent = Intent(ACTION_EVENTO_IOT)
        intent.putExtra("tipo_evento", evento)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun construirYMostrarNotificacion(title: String, body: String, tipoEvento: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmas Críticas (Sismo/Incendio)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de seguridad del hogar"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setBypassDnd(true)
                enableVibration(false) // Desactivado aquí para no cortar la manual
                vibrationPattern = null
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, LightControlActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (tipoEvento.isNotEmpty()) putExtra("auto_event", tipoEvento)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fire_on)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0L))

        notificationManager.notify(999, builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseMessaging.getInstance().subscribeToTopic("emergencias")
    }
}