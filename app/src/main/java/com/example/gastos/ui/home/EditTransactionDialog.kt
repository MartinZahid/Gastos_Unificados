package com.example.gastos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gastos.data.Transaction
import com.example.gastos.ui.common.amountWithoutSymbol
import com.example.gastos.ui.common.parseAmountInput
import com.example.gastos.ui.theme.BorderLine
import com.example.gastos.ui.theme.CardBackground
import com.example.gastos.ui.theme.Coral
import com.example.gastos.ui.theme.TextPrimary
import com.example.gastos.ui.theme.TextSecondary
import com.example.gastos.ui.theme.Volt

// Edición/borrado de un movimiento existente. Reusa parseAmountInput para
// aceptar "$1,250.00" igual que el formulario del Modo dev.
@Composable
internal fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: () -> Unit
) {
    var merchant by remember(transaction) { mutableStateOf(transaction.merchant) }
    var amountText by remember(transaction) { mutableStateOf(amountWithoutSymbol(transaction.amount)) }
    var bank by remember(transaction) { mutableStateOf(transaction.bank) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Editar movimiento", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Comercio", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = dialogFieldColors()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(color = TextPrimary),
                    colors = dialogFieldColors()
                )
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Banco", color = TextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
                    colors = dialogFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = parseAmountInput(amountText)
                if (amount != null && amount > 0 && merchant.isNotBlank() && bank.isNotBlank()) {
                    onSave(
                        transaction.copy(
                            merchant = merchant.trim(),
                            amount = amount,
                            bank = bank.trim()
                        )
                    )
                }
            }) {
                Text("Guardar", color = Volt, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = Coral, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Volt,
    unfocusedBorderColor = BorderLine,
    focusedLabelColor = Volt,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Volt
)