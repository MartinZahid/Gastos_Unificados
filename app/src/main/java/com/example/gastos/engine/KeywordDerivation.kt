package com.example.gastos.engine

private val triggerStopWords = setOf(
    "de", "del", "la", "el", "los", "las", "en", "por", "con", "a", "para", "tu", "su",
    "un", "una", "y", "o", "al", "que", "se", "es", "fue", "del", "notificacion",
    "banco", "tarjeta", "tdc", "credito", "crédito", "importante", "aviso", "mensaje",
    "cliente", "hola", "estimado", "estimada", "sr", "sra"
)

private val purchaseMarkers = listOf(
    "compra", "pago", "pagaste", "compraste", "autoriz", "aprob", "cargo",
    "retiro/compra", "establecimiento"
)

// Deduce la frase que antecede al comercio dentro del texto para aprenderla
// como patrón de compra (ej: "Retiro/Compra COSTCO …" -> "retiro/compra").
// Devuelve null si no hay contexto útil antes del comercio.
fun deriveTrigger(text: String, merchant: String): String? {
    val mFirst = merchant.split(Regex("\\s+")).firstOrNull()?.trim() ?: return null
    if (mFirst.isBlank()) return null
    val idx = text.indexOf(mFirst, ignoreCase = true)
    if (idx <= 0) return null
    val before = text.substring(0, idx)
    val words = before.split(Regex("\\s+"))
        .map { it.trim().trimEnd(',', ':', ';', '/') }
        .filter { it.isNotBlank() }
    val meaningful = words.filter { it.length >= 4 && it.lowercase() !in triggerStopWords }
    return when {
        meaningful.size >= 2 -> meaningful.takeLast(2).joinToString(" ").lowercase()
        meaningful.size == 1 -> meaningful.last().lowercase()
        else -> null
    }
}

// Sugiere una palabra para IGNORAR una notificación: la primera palabra
// alfabética larga del texto, salvo que el texto parezca una compra.
fun deriveIgnoreKeyword(text: String): String? {
    if (purchaseMarkers.any { text.contains(it, ignoreCase = true) }) return null
    val first = text.split(Regex("\\s+"))
        .firstOrNull { it.length >= 4 && it.all { c -> c.isLetter() } }
        ?: return null
    return first.lowercase()
}