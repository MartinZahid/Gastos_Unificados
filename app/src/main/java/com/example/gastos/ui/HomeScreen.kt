package com.example.gastos.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gastos.data.Transaction
import com.example.gastos.ui.theme.BorderLine
import com.example.gastos.ui.theme.CardBackground
import com.example.gastos.ui.theme.CardElevated
import com.example.gastos.ui.theme.Coral
import com.example.gastos.ui.theme.DarkBackground
import com.example.gastos.ui.theme.Ink
import com.example.gastos.ui.theme.TextPrimary
import com.example.gastos.ui.theme.TextSecondary
import com.example.gastos.ui.theme.Volt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: TransactionViewModel = viewModel(factory = TransactionViewModel.Factory)
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val banks by viewModel.banks.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedBank by viewModel.selectedBank.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listenerEnabled = remember { isListenerEnabled(context) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var devOpen by remember { mutableStateOf(false) }

    if (devOpen) {
        DevScreen(
            viewModel = viewModel,
            onBack = { devOpen = false }
        )
    } else {
        ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = CardBackground,
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            ) {
                DrawerPanel(
                    query = query,
                    banks = banks,
                    selectedBank = selectedBank,
                    summary = summary,
                    onQueryChange = viewModel::setQuery,
                    onBankSelected = viewModel::selectBank,
                    onClose = { scope.launch { drawerState.close() } },
                    onOpenDev = {
                        scope.launch { drawerState.close() }
                        devOpen = true
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Header(
                listenerEnabled = listenerEnabled,
                onOpenMenu = { scope.launch { drawerState.open() } },
                onOpenSettings = { openNotificationSettings(context) }
            )

            Spacer(Modifier.height(18.dp))

            HeroCard(summary = summary)

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    label = "ESTE MES",
                    value = formatMoney(summary.thisMonth),
                    valueColor = Coral,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "MAYOR GASTO",
                    value = formatMoney(summary.max),
                    valueColor = Coral,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            Controls(
                onOpenSettings = { openNotificationSettings(context) },
                onSimulate = { viewModel.simulateTransaction() }
            )

            Spacer(Modifier.height(14.dp))

            TransactionsContainer(
                transactions = transactions,
                modifier = Modifier.weight(1f),
                onTransactionClick = { editing = it }
            )
        }
    }
    }

    editing?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.save(updated)
                editing = null
            },
            onDelete = {
                viewModel.delete(transaction)
                editing = null
            }
        )
    }
}

@Composable
private fun Header(
    listenerEnabled: Boolean,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Abrir filtros",
                tint = TextPrimary
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "TDC · 100% LOCAL",
                color = Volt,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Consolidado de Gastos",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
        StatusChip(
            listenerEnabled = listenerEnabled,
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun StatusChip(listenerEnabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = CardBackground,
        border = BorderStroke(1.dp, if (listenerEnabled) Volt else Coral)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (listenerEnabled) Volt else Coral)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (listenerEnabled) "Activa" else "Sin permiso",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DrawerPanel(
    query: String,
    banks: List<String>,
    selectedBank: String?,
    summary: Summary,
    onQueryChange: (String) -> Unit,
    onBankSelected: (String?) -> Unit,
    onClose: () -> Unit,
    onOpenDev: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Filtros",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "y análisis",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        SearchField(query = query, onQueryChange = onQueryChange)

        Spacer(Modifier.height(20.dp))

        DrawerLabel("BANCO")
        Spacer(Modifier.height(8.dp))
        DrawerBankOption(
            label = "Todos",
            selected = selectedBank == null,
            onClick = { onBankSelected(null) }
        )
        banks.forEach { bank ->
            DrawerBankOption(
                label = bank,
                selected = selectedBank == bank,
                onClick = { onBankSelected(bank) }
            )
        }

        Spacer(Modifier.height(20.dp))

        DrawerLabel("RESUMEN")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DrawerStat(
                label = "Movimientos",
                value = summary.count.toString(),
                modifier = Modifier.weight(1f)
            )
            DrawerStat(
                label = "Promedio",
                value = formatMoney(summary.average),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        DrawerLabel("POR BANCO")
        Spacer(Modifier.height(12.dp))
        BankBreakdown(shares = summary.byBank)

        Spacer(Modifier.weight(1f))

        Surface(
            onClick = onOpenDev,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = CardElevated,
            border = BorderStroke(1.dp, BorderLine)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modo dev",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Toca un movimiento para editarlo o eliminarlo.",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DrawerLabel(text: String) {
    Text(
        text = text,
        color = Volt,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun DrawerBankOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Volt else CardElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (selected) Ink else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardElevated)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardElevated,
        border = BorderStroke(1.dp, BorderLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Buscar comercio…", color = TextSecondary, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeroCard(summary: Summary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CornerCutShape())
            .background(Volt)
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Text(
            text = "TOTAL GASTADO",
            color = Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatMoney(summary.total),
            color = Ink,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "${summary.count} movimientos · promedio ${formatMoney(summary.average)}",
            color = Ink.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CornerCutShape(radius = 18.dp, cut = 20.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val bankColors = listOf(Volt, Coral, Color(0xFF7CC6FF), Color(0xFFC9A2FF))

@Composable
private fun BankBreakdown(shares: List<BankShare>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(CardElevated)
        ) {
            shares.forEachIndexed { index, share ->
                Box(
                    Modifier
                        .weight(share.total.toFloat())
                        .fillMaxHeight()
                        .background(bankColors[index % bankColors.size])
                )
            }
        }

        if (shares.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Sin datos para este filtro", color = TextSecondary, fontSize = 12.sp)
        } else {
            Spacer(Modifier.height(12.dp))
            shares.take(3).forEach { share ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val index = shares.indexOf(share)
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(bankColors[index % bankColors.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = share.bank,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatMoney(share.total),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            if (shares.size > 3) {
                Text(
                    text = "+${shares.size - 3} más",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Controls(onOpenSettings: () -> Unit, onSimulate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onOpenSettings,
            modifier = Modifier.weight(1f),
            shape = CornerCutShape(radius = 18.dp, cut = 20.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, BorderLine)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Permisos", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Surface(
            onClick = onSimulate,
            modifier = Modifier.weight(1f),
            shape = CornerCutShape(radius = 18.dp, cut = 20.dp),
            color = Volt
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Simular", color = Ink, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun TransactionsContainer(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    onTransactionClick: (Transaction) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CornerCutShape())
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MOVIMIENTOS",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f)
            )
            Surface(shape = RoundedCornerShape(50), color = CardElevated) {
                Text(
                    text = transactions.size.toString(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin movimientos.\nToca Simular para probar.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = CardElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${transaction.bank} · ${formatDate(transaction.dateMillis)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                text = formatMoney(transaction.amount),
                color = Coral,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun EditTransactionDialog(
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
                val amount = amountText.replace(",", "").toDoubleOrNull()
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

private class CornerCutShape(
    private val radius: Dp = 20.dp,
    private val cut: Dp = 26.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { radius.toPx() }
        val c = with(density) { cut.toPx() }
        val path = Path().apply {
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(size.width - r, 0f)
            quadraticTo(size.width, 0f, size.width, r)
            lineTo(size.width - c, size.height)
            lineTo(r, size.height)
            quadraticTo(0f, size.height, 0f, size.height - r)
            close()
        }
        return Outline.Generic(path)
    }
}

private fun formatMoney(value: Double): String =
    String.format(Locale.US, "$%,.2f", value)

private fun amountWithoutSymbol(value: Double): String =
    String.format(Locale.US, "%.2f", value)

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM · HH:mm", Locale("es", "MX"))
    return formatter.format(Date(millis))
}

private fun isListenerEnabled(context: Context): Boolean {
    val packages = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packages.contains(context.packageName)
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}