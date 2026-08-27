package com.Flood.gastometro.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Formateo y parseo de montos/fechas compartido por toda la UI,
// para que un cambio de formato se aplique en un solo lugar.

internal fun formatMoney(value: Double): String =
    String.format(Locale.US, "$%,.2f", value)

internal fun amountWithoutSymbol(value: Double): String =
    String.format(Locale.US, "%.2f", value)

internal fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM · HH:mm", Locale("es", "MX"))
    return formatter.format(Date(millis))
}

// Normaliza la entrada de un monto escrito por el usuario:
// "$1,250.00" -> 1250.0, "150.50" -> 150.5.
internal fun parseAmountInput(text: String): Double? =
    text.replace(",", "").replace("$", "").trim().toDoubleOrNull()