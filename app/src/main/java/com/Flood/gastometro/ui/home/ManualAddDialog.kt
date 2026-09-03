package com.Flood.gastometro.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.Flood.gastometro.ui.common.parseAmountInput
import com.Flood.gastometro.ui.theme.BorderLine
import com.Flood.gastometro.ui.theme.CardBackground
import com.Flood.gastometro.ui.theme.TextPrimary
import com.Flood.gastometro.ui.theme.TextSecondary
import com.Flood.gastometro.ui.theme.Volt

// Alta manual de un gasto desde la pantalla principal. Reusa parseAmountInput
// para aceptar "$1,250.00" igual que los demás formularios.
@Composable
internal fun ManualAddDialog(
    onDismiss: () -> Unit,
    onSave: (merchant: String, amount: Double, bank: String) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("Otro") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        title = {
            Text("Agregar gasto", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Comercio", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = manualAddFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(color = TextPrimary),
                    colors = manualAddFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Banco", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = manualAddFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = parseAmountInput(amountText)
                if (amount != null && amount > 0 && merchant.isNotBlank() && bank.isNotBlank()) {
                    onSave(merchant.trim(), amount, bank.trim())
                }
            }) {
                Text("Guardar", color = Volt, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun manualAddFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Volt,
    unfocusedBorderColor = BorderLine,
    focusedLabelColor = Volt,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Volt
)
