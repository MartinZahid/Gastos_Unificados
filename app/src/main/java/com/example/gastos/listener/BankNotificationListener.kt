package com.example.gastos.listener

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
            db.notificationLogDao().prune()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
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