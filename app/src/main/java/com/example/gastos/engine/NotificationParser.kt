package com.example.gastos.engine

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

    private val wordClass = "[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9&'.,-]"
    private val merchantCapture = "$wordClass+(?:\\s+$wordClass+){0,6}"

    private val amountPatterns = listOf(
        Regex("""[$]\s*([\d.,]+)"""),
        Regex("""(?:MXN|MN|USD|US)\s*[$]\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:monto|importe|total)\s*:?\s*[$]?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""([\d.,]+)\s*(?:MXN|MN|USD|pesos)""", RegexOption.IGNORE_CASE)
    )

    private val merchantPatterns = listOf(
        Regex(
            """(?:comercio|establecimiento|adquiriente)\s*:?\s*($merchantCapture)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:compra en|compra por|compra aprobada en|compra aprobada por|compra realizada en|compra realizada por)\s*[:\-\s]*($merchantCapture)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:aprobada en|aprobada por|autorizado en|autorizada en|autorizado por|autorizada por)\s*[:\-\s]*($merchantCapture)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:pago en|pago a|pagaste en|pagaste a|compraste en|compraste a)\s*[:\-\s]*($merchantCapture)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:cargo en|cargo por|realizado en|realizada en|realizado por|realizada por|retiro/compra|compra realizada)\s*[:\-\s]*($merchantCapture)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """compra\s+($merchantCapture)""",
            RegexOption.IGNORE_CASE
        )
    )

    private val fallbackMerchantRegex = Regex(
        """($merchantCapture)\s+(?:por|monto|importe)\s*[$]?\s*""",
        RegexOption.IGNORE_CASE
    )

    private val boundaryRegex = Regex(
        """(?i)\s+(?:monto|importe|total|por|con|de|el|la|los|las|a|a las|auto\.?|ref\.?|referencia|folio|tarjeta|cuenta|mxn|mn|usd|pesos)\b|\s+[A-Za-z]*[0-9]+[A-Za-z0-9]*|\s+\d{1,2}/\d{1,2}|\s*[$][\d.,]+"""
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
            else -> null
        }
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text) ?: continue
            val cleaned = match.groupValues[1].replace(",", "").trim()
            cleaned.toDoubleOrNull()?.let { return it }
        }
        return null
    }

    private fun extractMerchant(text: String, extraKeywords: List<String>): String? {
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
        return s.takeIf { it.length >= 2 }
    }
}