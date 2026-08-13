package com.example.gastos.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gastos.data.Transaction
import com.example.gastos.ui.DevScreen
import com.example.gastos.ui.TransactionViewModel
import com.example.gastos.ui.common.CornerCutShape
import com.example.gastos.ui.common.formatMoney
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

private fun isListenerEnabled(context: Context): Boolean {
    val packages = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packages.contains(context.packageName)
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}