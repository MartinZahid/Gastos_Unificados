package com.example.gastos.listener

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

        val packageName = sbn.packageName
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
            val bank = BankNames[packageName] ?: packageName

            // Registramos TODAS las notificaciones (parseadas o no) para depurar
            // patrones nuevos desde DevScreen; solo creamos una Transaction si el
            // paquete está en la lista de apps bancarias soportadas (inTarget),
            // para no ensuciar los movimientos con avisos de apps ajenas.
            when (result) {
                is ParseResult.Success -> {
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
                    db.notificationLogDao().insert(
                        NotificationLog(
                            packageName = packageName,
                            title = title,
                            text = text,
                            parsed = false,
                            reason = result.reason,
                            inTargetList = inTarget
                        )
                    )
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

    companion object {
        // Canal e id de la notificación permanente de estado del servicio.
        private const val STATUS_CHANNEL_ID = "listener_status"
        private const val STATUS_NOTIFICATION_ID = 1

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