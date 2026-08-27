package com.Flood.gastometro.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Flood.gastometro.data.LearnedPattern
import com.Flood.gastometro.data.NotificationLog
import com.Flood.gastometro.engine.NotificationParser
import com.Flood.gastometro.engine.ParseResult
import com.Flood.gastometro.ui.common.formatMoney
import com.Flood.gastometro.ui.common.parseAmountInput
import com.Flood.gastometro.ui.theme.BorderLine
import com.Flood.gastometro.ui.theme.CardBackground
import com.Flood.gastometro.ui.theme.CardElevated
import com.Flood.gastometro.ui.theme.Coral
import com.Flood.gastometro.ui.theme.Ink
import com.Flood.gastometro.ui.theme.TextPrimary
import com.Flood.gastometro.ui.theme.TextSecondary
import com.Flood.gastometro.ui.theme.Volt

@Composable
internal fun ParserTesterDialog(
    examples: List<String>,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(examples.first()) }
    var result by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Probar parser", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Texto", color = TextSecondary) },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    colors = devFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                result?.let {
                    Text(
                        it,
                        color = if (it.startsWith("OK")) Volt else Coral,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text("Ejemplos:", color = TextSecondary, fontSize = 11.sp)
                examples.forEach { ex ->
                    Surface(
                        onClick = { input = ex },
                        shape = RoundedCornerShape(8.dp),
                        color = CardElevated
                    ) {
                        Text(
                            ex,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val res = NotificationParser.parse(input)
                    result = when (res) {
                        is ParseResult.Success ->
                            "OK · ${res.purchase.merchant} · ${formatMoney(res.purchase.amount)}" +
                                (res.purchase.bank?.let { " · $it" } ?: "")
                        is ParseResult.Failure -> "NO · ${res.reason}"
                    }
                }) { Text("Probar", color = Volt, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onDismiss) { Text("Cerrar", color = TextSecondary) }
            }
        }
    )
}

@Composable
internal fun ManualPurchaseDialog(
    log: NotificationLog,
    onDismiss: () -> Unit,
    onSave: (merchant: String, amountText: String) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf(guessAmount(log.text)) }
    var bank by remember { mutableStateOf(log.bank ?: "Otro") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Agregar compra", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Comercio", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = devFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(color = TextPrimary),
                    colors = devFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Banco", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = devFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Se aprenderá la frase que antecede al comercio.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // parseAmountInput acepta "$110.00" / "1,250" (igual que el ViewModel);
                // toDoubleOrNull devuelve null con el símbolo y el botón no respondía.
                if (merchant.isNotBlank() && parseAmountInput(amountText)?.let { it > 0 } == true) {
                    onSave(merchant.trim(), amountText.trim())
                }
            }) { Text("Guardar", color = Volt, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        }
    )
}

@Composable
internal fun TypeDialog(
    initial: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var selected by remember { mutableStateOf(initial ?: "") }
    var custom by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Tipo", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fixedTypes.forEach { type ->
                    val isSel = selected == type
                    Surface(
                        onClick = { selected = type; custom = "" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Volt else CardElevated
                    ) {
                        Text(
                            type,
                            color = if (isSel) Ink else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it; if (it.isNotBlank()) selected = "" },
                    label = { Text("Tipo personalizado", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = devFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val final = if (custom.isNotBlank()) custom.trim() else selected
                if (final.isNotBlank()) onSave(final)
            }) { Text("Guardar", color = Volt, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        }
    )
}

@Composable
internal fun LearnDialog(
    onDismiss: () -> Unit,
    onSave: (keyword: String, kind: String) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(LearnedPattern.COMPRA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Aprender frase", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Frase clave", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = devFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { kind = LearnedPattern.COMPRA },
                        shape = RoundedCornerShape(12.dp),
                        color = if (kind == LearnedPattern.COMPRA) Volt else CardElevated
                    ) {
                        Text(
                            "COMPRA",
                            color = if (kind == LearnedPattern.COMPRA) Ink else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                    Surface(
                        onClick = { kind = LearnedPattern.IGNORAR },
                        shape = RoundedCornerShape(12.dp),
                        color = if (kind == LearnedPattern.IGNORAR) Coral else CardElevated
                    ) {
                        Text(
                            "IGNORAR",
                            color = if (kind == LearnedPattern.IGNORAR) Ink else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (keyword.isNotBlank()) onSave(keyword.trim(), kind)
            }) { Text("Aprender", color = Volt, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        }
    )
}

@Composable
private fun devFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Volt,
    unfocusedBorderColor = BorderLine,
    focusedLabelColor = Volt,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Volt
)