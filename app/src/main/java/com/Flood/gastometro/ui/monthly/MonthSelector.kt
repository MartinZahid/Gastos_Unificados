package com.Flood.gastometro.ui.monthly

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val displayFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
private val parseFormat = SimpleDateFormat("yyyy-MM", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelector(
    months: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = remember(selected) {
        if (selected.isBlank()) "Seleccionar mes"
        else try { displayFormat.format(parseFormat.parse(selected)!!) }
        catch (_: Exception) { selected }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Mes") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            months.forEach { month ->
                val label = remember(month) {
                    try { displayFormat.format(parseFormat.parse(month)!!) }
                    catch (_: Exception) { month }
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(month)
                        expanded = false
                    }
                )
            }
        }
    }
}
