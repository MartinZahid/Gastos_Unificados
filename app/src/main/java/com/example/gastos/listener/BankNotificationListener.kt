package com.example.gastos.listener

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gastos.MainActivity
import com.example.gastos.R
import com.example.gastos.data.AppDatabase
import com.example.gastos.data.LearnedPattern
import com.example.gastos.data.NotificationLog
import com.example.gastos.data.Transaction
import com.example.gastos.engine.NotificationParser
import com.example.gastos.engine.ParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                STATUS_CHANNEL_ID,
                "Estado del servicio",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // Al conectarse publicamos una notificación permanente de bajo perfil:
    // con una notificación visible el sistema considera la app "en uso" y es
    // mucho menos probable que la duerma/congele (causa típica en Samsung de
    // dejar de capturar notificaciones).
    override fun onListenerConnected() {
        super.onListenerConnected()
        postStatusNotification()
    }

    // Si el sistema nos desconecta, ocultamos el estado y pedimos reconexión.
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationManagerCompat.from(this).cancel(STATUS_NOTIFICATION_ID)
        requestRebind(ComponentName(this, BankNotificationListener::class.java))
    }

    private fun postStatusNotification() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Escucha activa")
            .setContentText("Capturando notificaciones de tus bancos")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()
        NotificationManagerCompat.from(this).notify(STATUS_NOTIFICATION_ID, notification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // No procesamos las notificaciones que publica el propio servicio
        // (estado permanente y alerta de no reconocidas): el listener también
        // las recibe y, al no tener monto, se contarían como fallos falsos
        // (un falso "sin monto" por cada reconexión).
        val channelId = sbn.notification?.channelId
        if (channelId == STATUS_CHANNEL_ID || channelId == ALERT_CHANNEL_ID) return

        val packageName = sbn.packageName
        if (packageName in IgnoredPackages) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        scope.launch {
            val db = AppDatabase.getInstance(this@BankNotificationListener)
            val patterns = db.learnedPatternDao().getAll()
            val extraKeywords = patterns.filter { it.kind == LearnedPattern.COMPRA }.map { it.keyword }
            val ignoreKeywords = patterns.filter { it.kind == LearnedPattern.IGNORAR }.map { it.keyword }

            val result = NotificationParser.parse("$title $text", extraKeywords, ignoreKeywords)
            val inTarget = packageName in TargetPackages

            // Registramos TODAS las notificaciones (parseadas o no) para depurar
            // patrones nuevos desde DevScreen; solo creamos una Transaction si el
            // paquete está en la lista de apps bancarias soportadas (inTarget),
            // para no ensuciar los movimientos con avisos de apps ajenas.
            when (result) {
                is ParseResult.Success -> {
                    // Banco por paquete, con respaldo al banco detectado en el
                    // texto (p. ej. "RappiCard" -> Rappi) para apps aún no mapeadas.
                    val bank = BankNames[packageName] ?: result.purchase.bank ?: packageName
                    db.notificationLogDao().insert(
                        NotificationLog(
                            packageName = packageName,
                            title = title,
                            text = text,
                            parsed = true,
                            merchant = result.purchase.merchant,
                            amount = result.purchase.amount,
                            bank = bank,
                            inTargetList = inTarget
                        )
                    )
                    if (inTarget) {
                        db.transactionDao().insert(
                            Transaction(
                                merchant = result.purchase.merchant,
                                amount = result.purchase.amount,
                                bank = bank,
                                dateMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }
                is ParseResult.Failure -> {
                    val bank = BankNames[packageName] ?: packageName
                    db.notificationLogDao().insert(
                        NotificationLog(
                            packageName = packageName,
                            title = title,
                            text = text,
                            parsed = false,
                            reason = result.reason,
                            bank = bank,
                            inTargetList = inTarget
                        )
                    )
                    // Solo alertamos por fallos de apps bancarias soportadas:
                    // si un banco cambia el formato de su notificación, el
                    // regex deja de reconocerla y el gasto se perdería en
                    // silencio si nadie revisa Modo dev por su cuenta.
                    if (inTarget) maybeAlertUnreviewedFailures()
                }
            }
            // El log se mantiene acotado a las últimas 200 entradas para que
            // la pantalla dev no crezca sin límite en memoria.
            db.notificationLogDao().prune()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // Si se acumulan varias notificaciones bancarias sin reconocer, avisa una
    // sola vez (con enfriamiento) en vez de una notificación por cada fallo,
    // que sería spam. El umbral y el enfriamiento evitan avisar por un caso
    // aislado (p. ej. una promoción rara) y molestar de más.
    private suspend fun maybeAlertUnreviewedFailures() {
        val db = AppDatabase.getInstance(this)
        val unreviewed = db.notificationLogDao().unreviewedFailureCount()
        if (unreviewed < UNREVIEWED_ALERT_THRESHOLD) return

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastAlert = prefs.getLong(PREF_LAST_ALERT_MILLIS, 0L)
        val now = System.currentTimeMillis()
        if (now - lastAlert < ALERT_COOLDOWN_MILLIS) return
        prefs.edit().putLong(PREF_LAST_ALERT_MILLIS, now).apply()

        postUnreviewedAlert(unreviewed)
    }

    private fun postUnreviewedAlert(count: Int) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Notificaciones sin reconocer",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Gastos sin reconocer")
            .setContentText("No pude leer $count notificación(es) de tu banco. Revísalas en Modo dev.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(this).notify(ALERT_NOTIFICATION_ID, notification)
    }

    companion object {
        // Canal e id de la notificación permanente de estado del servicio.
        private const val STATUS_CHANNEL_ID = "listener_status"
        private const val STATUS_NOTIFICATION_ID = 1

        // Canal e id de la alerta proactiva de notificaciones sin reconocer.
        private const val ALERT_CHANNEL_ID = "unreviewed_failures"
        private const val ALERT_NOTIFICATION_ID = 2

        internal const val PREFS_NAME = "listener_prefs"
        internal const val PREF_LAST_ALERT_MILLIS = "last_unreviewed_alert_millis"

        // Mínimo de fallos acumulados antes de avisar.
        private const val UNREVIEWED_ALERT_THRESHOLD = 3
        // No repetir el aviso antes de que pase este tiempo, aunque sigan
        // llegando fallos nuevos.
        private const val ALERT_COOLDOWN_MILLIS = 12 * 60 * 60 * 1000L // 12h

        // Paquetes del sistema/OS que generan notificaciones ajenas a
        // transacciones bancarias (batería, routines Samsung, etc.).
        // Se ignoran para no ensuciar el log de Modo dev.
        private val IgnoredPackages = setOf(
            "com.android.systemui",
            "com.samsung.android.app.routines",
            "com.samsung.android.server.notification",
            "com.samsung.android.wifi.largetcpbuffer.resources",
            "com.sec.android.app.launcher"
        )

        // Paquetes de apps bancarias cuyas notificaciones generan movimientos.
        val TargetPackages: Set<String> = setOf(
            "com.nu.production",
            "com.mercadopago.wallet",
            "com.bancomer.mbanking",
            "com.bbva.bancomer.app",
            "mx.bancosantander.supermovil",
            "com.santander.latam.mx",
            "com.banamex.banamex",
            "com.example.gastos"
        )

        val BankNames: Map<String, String> = mapOf(
            "com.nu.production" to "Nubank",
            "com.mercadopago.wallet" to "Mercado Pago",
            "com.bancomer.mbanking" to "BBVA",
            "com.bbva.bancomer.app" to "BBVA",
            "mx.bancosantander.supermovil" to "Santander",
            "com.santander.latam.mx" to "Santander",
            "com.banamex.banamex" to "Citibanamex",
            "com.example.gastos" to "Pruebas"
        )
    }
}