package com.Flood.gastometro.engine

data class ParsedPurchase(
    val amount: Double,
    val merchant: String,
    val bank: String? = null
)

sealed class ParseResult {
    data class Success(val purchase: ParsedPurchase) : ParseResult()
    data class Failure(val reason: String) : ParseResult()
}

object NotificationParser {

    // Carácter válido dentro del nombre de un comercio. Se captura un máximo
    // de 7 palabras para no tragarse el resto de la notificación.
    private val wordClass = "[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9&'.,-]"
    private val merchantCapture = "$wordClass+(?:\\s+$wordClass+){0,6}"

    // --- Patrones de monto, en orden de prioridad ---
    // Ej: "Retiro/Compra COSTCO … monto $110.00" o "Compra aprobada en OXXO por $85.50"
    private val dollarAmount = Regex("""[$]\s*([\d.,]+)""")
    // Ej: "MXN $500.00", "USD $12.00"
    private val currencyDollar = Regex("""(?:MXN|MN|USD|US)\s*[$]\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    // Ej: "monto $110.00", "importe total: 150.00"
    private val labeledAmount = Regex("""(?:monto|importe|total)\s*:?\s*[$]?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    // Ej: "500.00 MXN", "150.50 pesos"
    private val trailingCurrency = Regex("""([\d.,]+)\s*(?:MXN|MN|USD|pesos)""", RegexOption.IGNORE_CASE)

    private val amountPatterns = listOf(dollarAmount, currencyDollar, labeledAmount, trailingCurrency)

    // --- Patrones de comercio, en orden de prioridad ---
    // Ej: "Comercio WALMART SUPERMERCADO monto $999.99"
    private val labeledMerchant = Regex(
        """(?:comercio|establecimiento|adquiriente)\s*:?\s*($merchantCapture)""",
        RegexOption.IGNORE_CASE
    )
    // Ej: "Compra aprobada en OXXO por $85.50 con tarjeta BANAMEX512"
    private val compraEn = Regex(
        """(?:compra en|compra por|compra aprobada en|compra aprobada por|compra realizada en|compra realizada por)\s*[:\-\s]*($merchantCapture)""",
        RegexOption.IGNORE_CASE
    )
    // Ej: "Pago autorizado en SORIANA $1,250.00"
    private val aprobadoEn = Regex(
        """(?:aprobada en|aprobada por|autorizado en|autorizada en|autorizado por|autorizada por)\s*[:\-\s]*($merchantCapture)""",
        RegexOption.IGNORE_CASE
    )
    // Ej: "Compraste en NETFLIX $239.00 con tu tarjeta NU"
    private val pagasteEn = Regex(
        """(?:pago en|pago a|pagaste en|pagaste a|compraste en|compraste a)\s*[:\-\s]*($merchantCapture)""",
        RegexOption.IGNORE_CASE
    )
    // Ej: "Pagaste $109.00 en Carls Jr con tu RappiCard digital." El monto va
    // entre el verbo y el comercio; por eso el monto es opcional antes del "en/a/por".
    private val verboConMonto = Regex(
        """(?:pagaste|compraste|abonaste|gastaste)\s*(?:[$]?[\d.,]+\s*)?(?:en|a|por)\s*[:\-\s]*($merchantCapture)""",
        RegexOption.IGNORE_CASE
    )
    // Ej: "Retiro/Compra COSTCO HERMOSILLO HER COSTCO BANAMEX512 monto $110.00"
    private val cargoOCompra = Regex(
        """(?:cargo en|cargo por|realizado en|realizada en|realizado por|realizada por|retiro/compra|compra realizada)\s*[:\-\s]*($merchantCapture)""",
        RegexOption.IGNORE_CASE
    )
    // Ej: "Compra Cinepolis monto $80.00"
    private val compraBare = Regex(
        """compra\s+($merchantCapture)""",
        RegexOption.IGNORE_CASE
    )

    private val merchantPatterns = listOf(
        labeledMerchant, compraEn, aprobadoEn, pagasteEn, verboConMonto, cargoOCompra, compraBare
    )

    // Último intento: captura las palabras previas a "por/monto/importe" seguidos de monto.
    // Ej: "Transferencia recibida de JUAN PEREZ por $500.00 MXN"
    private val fallbackMerchantRegex = Regex(
        """($merchantCapture)\s+(?:por|monto|importe)\s*[$]?\s*""",
        RegexOption.IGNORE_CASE
    )

    // --- Cortes de comercio (dónde termina el nombre del comercio) ---
    // Palabras que separan el comercio del resto: "COSTCO … monto $110.00"
    private val boundaryWords = Regex(
        """\s+(?:monto|importe|total|por|con|de|el|la|los|las|a|a las|auto\.?|ref\.?|referencia|folio|tarjeta|cuenta|mxn|mn|usd|pesos)\b""",
        RegexOption.IGNORE_CASE
    )
    // Tokens que mezclan texto y dígitos: "COSTCO … BANAMEX512 monto …" (últimos 4 del TDC)
    private val boundaryMixedTokens = Regex("""\s+[A-Za-z]*[0-9]+[A-Za-z0-9]*""")
    // Fechas: "el 12/08/26 04:15:13 PM"
    private val boundaryDates = Regex("""\s+\d{1,2}/\d{1,2}""")
    // Montos en línea: "SORIANA $1,250.00"
    private val boundaryAmounts = Regex("""\s*[$][\d.,]+""")

    private val boundaryRegex = Regex(
        listOf(boundaryWords, boundaryMixedTokens, boundaryDates, boundaryAmounts).joinToString("|")
    )

    // Relleno que algunos patrones capturan antes del verdadero comercio:
    // "Compra con tu tarjeta ..." -> captura "CON TU TARJETA" -> corte en
    // "tarjeta" deja "CON TU". Se elimina al inicio quedando el comercio
    // real si lo hay; si solo era relleno, la captura queda vacía y la
    // notificación pasa a fallo (revisión en Modo dev) en vez de crear un
    // movimiento con basura como comercio.
    private val leadingFiller = Regex(
        """^(?:(?:con|de|en|por)\s+(?:tu|su)|(?:con|tu|su|de|en|por)\s+)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        rawText: String,
        extraKeywords: List<String> = emptyList(),
        ignoreKeywords: List<String> = emptyList()
    ): ParseResult {
        val ignored = ignoreKeywords.firstOrNull { rawText.contains(it, ignoreCase = true) }
        if (ignored != null) {
            return ParseResult.Failure("ignorado: $ignored")
        }
        val amount = extractAmount(rawText) ?: return ParseResult.Failure("sin monto")
        val merchant = extractMerchant(rawText, extraKeywords) ?: return ParseResult.Failure("sin comercio")
        return ParseResult.Success(
            ParsedPurchase(amount = amount, merchant = merchant, bank = detectBank(rawText))
        )
    }

    fun detectBank(text: String): String? {
        return when {
            text.contains("BANAMEX", true) -> "Citibanamex"
            text.contains("BANORTE", true) -> "Banorte"
            text.contains("BBVA", true) -> "BBVA"
            text.contains("SANTANDER", true) || text.contains("SUPERMOVIL", true) -> "Santander"
            text.contains("MERCADO", true) -> "Mercado Pago"
            text.contains("NUBANK", true) || Regex("""\bNU\b""").containsMatchIn(text) -> "Nubank"
            text.contains("RAPPI", true) -> "Rappi"
            else -> null
        }
    }

    // Expone el monto como texto (formato original) para reusar la misma lógica
    // del parser en la UI (p. ej. prellenar el formulario manual del Modo dev),
    // en lugar de mantener una segunda implementación de "extraer monto".
    fun extractAmountText(rawText: String): String? {
        for (pattern in amountPatterns) {
            val match = pattern.find(rawText) ?: continue
            val cleaned = match.groupValues[1].replace(",", "").trim()
            if (cleaned.toDoubleOrNull() != null) return cleaned
        }
        return null
    }

    private fun extractAmount(text: String): Double? =
        extractAmountText(text)?.toDoubleOrNull()

    private fun extractMerchant(text: String, extraKeywords: List<String>): String? {
        // Las frases aprendidas desde Modo dev se prueban después de los
        // patrones fijos: ej, aprendida "retiro/compra" -> "retiro/compra COSTCO …".
        val patterns = merchantPatterns + extraKeywords.map { learnedMerchantRegex(it) }
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            cleanMerchant(match.groupValues[1])?.let { return it }
        }
        val fallback = fallbackMerchantRegex.find(text) ?: return null
        return cleanMerchant(fallback.groupValues[1])
    }

    private fun learnedMerchantRegex(keyword: String): Regex {
        val escaped = Regex.escape(keyword)
        return Regex(
            escaped + """\s*[:\-\s]*($merchantCapture)""",
            RegexOption.IGNORE_CASE
        )
    }

    private fun cleanMerchant(raw: String): String? {
        var s = raw.trim()
        s = s.trimEnd(',', '.', ':', '-', ';', '(', ')', '!', '?', '·')
        val cut = boundaryRegex.find(s)
        if (cut != null) {
            s = s.substring(0, cut.range.first).trim()
        }
        s = s.trimEnd(',', '.', ':', '-', ';', '(', ')', '·')
        // Elimina relleno inicial ("con tu", "tu", "de", ...) que algunos
        // patrones capturan antes del comercio real.
        repeat(3) {
            val stripped = leadingFiller.replaceFirst(s, "").trim()
            if (stripped == s) return@repeat
            s = stripped
        }
        return s.takeIf { it.length >= 2 }
    }
}