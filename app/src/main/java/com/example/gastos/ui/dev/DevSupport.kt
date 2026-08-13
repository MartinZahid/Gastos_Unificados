package com.example.gastos.ui.dev

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gastos.R
import com.example.gastos.engine.NotificationParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val fixedTypes = listOf("Transferencia", "Saldo", "Promoción", "Retiro", "Otro")

internal val testCorpus = listOf(
    "Retiro/Compra COSTCO HERMOSILLO HER COSTCO BANAMEX512 monto \$110.00 el 12/08/26 04:15:13 PM. Auto. 792877",
    "Compra aprobada en OXXO por \$85.50 con tarjeta BANAMEX512",
    "Compraste en NETFLIX \$239.00 con tu tarjeta NU",
    "Pago autorizado en SORIANA \$1,250.00",
    "Comercio WALMART SUPERMERCADO monto \$999.99",
    "Compra realizada en Uber \$60.00",
    "Pagaste \$109.00 en Carls Jr con tu RappiCard digital.",
    // Deliberadamente sin reconocer: sirve para ver el caso "NO" del tester
    // y el banner de "notificaciones sin reconocer" al simularla.
    "Su compra no pudo completarse. Saldo insuficiente en la tarjeta terminación 512."
)

internal fun postTestNotification(context: Context, text: String) {
    val channelId = "dev_test"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Pruebas",
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Gastos · Prueba")
        .setContentText(text)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        notification
    )
}

// Prellenar el monto del formulario manual reusando el parser real, en vez de
// mantener una segunda implementación de "extraer monto de un texto" en la UI.
internal fun guessAmount(text: String): String =
    NotificationParser.extractAmountText(text) ?: ""

internal fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM HH:mm", Locale("es", "MX"))
    return formatter.format(Date(millis))
}