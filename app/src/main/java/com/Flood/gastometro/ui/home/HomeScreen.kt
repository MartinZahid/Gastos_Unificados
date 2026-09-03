package com.Flood.gastometro.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Flood.gastometro.data.Transaction
import com.Flood.gastometro.ui.dev.DevScreen
import com.Flood.gastometro.ui.monthly.MonthSelector
import com.Flood.gastometro.ui.monthly.MonthlyHistoryScreen
import com.Flood.gastometro.ui.TransactionViewModel
import com.Flood.gastometro.ui.common.CornerCutShape
import com.Flood.gastometro.ui.common.formatMoney
import com.Flood.gastometro.ui.theme.BorderLine
import com.Flood.gastometro.ui.theme.CardBackground
import com.Flood.gastometro.ui.theme.CardElevated
import com.Flood.gastometro.ui.theme.Coral
import com.Flood.gastometro.ui.theme.DarkBackground
import com.Flood.gastometro.ui.theme.Ink
import com.Flood.gastometro.ui.theme.TextPrimary
import com.Flood.gastometro.ui.theme.TextSecondary
import com.Flood.gastometro.ui.theme.Volt
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: TransactionViewModel = viewModel(factory = TransactionViewModel.Factory)
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val banks by viewModel.banks.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedBank by viewModel.selectedBank.collectAsStateWithLifecycle()
    val unreviewedFailureCount by viewModel.unreviewedFailureCount.collectAsStateWithLifecycle()
    val homeMonth by viewModel.homeMonth.collectAsStateWithLifecycle()
    val homeSummary by viewModel.homeSummary.collectAsStateWithLifecycle()
    val homeTransactions by viewModel.homeTransactions.collectAsStateWithLifecycle()
    val recentMonths by viewModel.recentMonths.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var listenerEnabled by remember { mutableStateOf(isListenerEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = isListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var devOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var showKeepAlive by remember { mutableStateOf(false) }
    var showManualAdd by remember { mutableStateOf(false) }

    // Pide el permiso de notificaciones al arrancar: necesario para la
    // notificación permanente que mantiene vivo el servicio de escucha.
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (devOpen) {
        DevScreen(
            viewModel = viewModel,
            onBack = { devOpen = false }
        )
    } else if (historyOpen) {
        MonthlyHistoryScreen(
            viewModel = viewModel,
            onBack = { historyOpen = false }
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
                        },
                        onOpenHistory = {
                            scope.launch { drawerState.close() }
                            historyOpen = true
                        },
                        onOpenKeepAlive = {
                            scope.launch { drawerState.close() }
                            showKeepAlive = true
                        },
                        unreviewedFailureCount = unreviewedFailureCount
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

                MonthSelector(
                    months = recentMonths,
                    selected = homeMonth,
                    onSelected = viewModel::selectHomeMonth,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                HeroCard(summary = homeSummary)

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricTile(
                        label = "MAYOR GASTO",
                        value = formatMoney(homeSummary.max),
                        valueColor = Coral,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "PROMEDIO",
                        value = formatMoney(homeSummary.average),
                        valueColor = Coral,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Controls(
                    listenerEnabled = listenerEnabled,
                    onOpenSettings = { openNotificationSettings(context) },
                    onAddExpense = { showManualAdd = true }
                )

                Spacer(Modifier.height(14.dp))

                TransactionsContainer(
                    transactions = homeTransactions,
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

    if (showKeepAlive) {
        KeepAliveDialog(
            ignoringOptimizations = isIgnoringBatteryOptimizations(context),
            onDismiss = { showKeepAlive = false },
            onOpenSettings = { openBatteryOptimizationSettings(context) }
        )
    }

    if (showManualAdd) {
        ManualAddDialog(
            onDismiss = { showManualAdd = false },
            onSave = { merchant, amount, bank ->
                viewModel.insertPurchase(merchant, amount, bank)
                showManualAdd = false
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
                text = "GASTOMETRO · 100% LOCAL",
                color = Volt,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "GastoMetro",
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
private fun Controls(
    listenerEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit
) {
    if (!listenerEnabled) {
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
                onClick = onAddExpense,
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
                    Text("Agregar gasto", color = Ink, fontWeight = FontWeight.Black)
                }
            }
        }
    } else {
        Surface(
            onClick = onAddExpense,
            modifier = Modifier.fillMaxWidth(),
            shape = CornerCutShape(radius = 18.dp, cut = 20.dp),
            color = Volt
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Agregar gasto", color = Ink, fontWeight = FontWeight.Black)
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

// Diálogo para evitar que Samsung "duerma" la app y deje de capturar
// notificaciones. En Samsung el permiso automático suele estar bloqueado,
// por eso se guía al usuario a los ajustes manuales + un enlace directo.
@Composable
private fun KeepAliveDialog(
    ignoringOptimizations: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("No dormir la app", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (ignoringOptimizations) {
                        "Ya está excluida de la optimización de batería."
                    } else {
                        "La app puede dormirse y dejar de capturar notificaciones."
                    },
                    color = if (ignoringOptimizations) Volt else Coral,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Configuración → Batería → Límites de uso en segundo plano → " +
                        "Apps que nunca se ponen en reposo → agrega 'GastoMetro'.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Abrir ajustes de batería", color = Volt, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextSecondary)
            }
        }
    )
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatteryOptimizationSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}