package com.example.gastos.ui.dev

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gastos.data.LearnedPattern
import com.example.gastos.data.NotificationLog
import com.example.gastos.engine.deriveIgnoreKeyword
import com.example.gastos.engine.deriveTrigger
import com.example.gastos.ui.TransactionViewModel
import com.example.gastos.ui.theme.CardElevated
import com.example.gastos.ui.theme.DarkBackground
import com.example.gastos.ui.theme.TextPrimary
import com.example.gastos.ui.theme.TextSecondary
import com.example.gastos.ui.theme.Volt

@Composable
fun DevScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.notificationLogs.collectAsStateWithLifecycle()
    val learned by viewModel.learnedPatterns.collectAsStateWithLifecycle()
    val unreviewedCount by viewModel.unreviewedFailureCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showTester by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf<NotificationLog?>(null) }
    var showType by remember { mutableStateOf<NotificationLog?>(null) }
    var showLearn by remember { mutableStateOf(false) }
    var corpusIndex by remember { mutableIntStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "MODO DEV",
                    color = Volt,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Alimentación del parser",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DevActionButton(
                icon = Icons.Default.Search,
                label = "Probar",
                onClick = { showTester = true },
                modifier = Modifier.weight(1f)
            )
            DevActionButton(
                icon = Icons.Default.Notifications,
                label = "Simular",
                onClick = {
                    postTestNotification(context, testCorpus[corpusIndex % testCorpus.size])
                    corpusIndex++
                },
                modifier = Modifier.weight(1f)
            )
            DevActionButton(
                icon = Icons.Default.Delete,
                label = "Limpiar",
                onClick = { viewModel.clearLogs() },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        DevLabel("FRASES APRENDIDAS")
        Spacer(Modifier.height(8.dp))
        LearnedSection(
            patterns = learned,
            onDelete = viewModel::deleteLearned,
            onAdd = { showLearn = true }
        )

        Spacer(Modifier.height(20.dp))

        // Aviso de que el parser dejó de reconocer notificaciones de un
        // banco soportado: sin esto, un cambio de formato del banco pierde
        // gastos en silencio hasta que alguien entra a revisar por su cuenta.
        if (unreviewedCount > 0) {
            UnreviewedFailuresBanner(
                count = unreviewedCount,
                onMarkReviewed = viewModel::markFailuresReviewed
            )
            Spacer(Modifier.height(16.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            DevLabel("LOG DE NOTIFICACIONES")
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = CardElevated) {
                Text(
                    text = logs.size.toString(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Sin notificaciones aún.\nToca Simular o activa la escucha en Permisos.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogRow(
                        log = log,
                        onCompra = {
                            if (log.parsed && log.merchant != null && log.amount != null) {
                                viewModel.insertPurchase(log.merchant, log.amount, log.bank ?: "Otro")
                            } else {
                                showManual = log
                            }
                        },
                        onIgnorar = {
                            val kw = deriveIgnoreKeyword(log.text)
                            if (kw != null) viewModel.learnIgnorar(kw)
                        },
                        onTipo = { showType = log }
                    )
                }
            }
        }
    }

    if (showTester) {
        ParserTesterDialog(
            examples = testCorpus,
            onDismiss = { showTester = false }
        )
    }

    showManual?.let { log ->
        ManualPurchaseDialog(
            log = log,
            onDismiss = { showManual = null },
            onSave = { merchant, amountText ->
                viewModel.insertManualPurchase(merchant, amountText, log.bank ?: "Otro")
                deriveTrigger(log.text, merchant)?.let { viewModel.learnCompra(it) }
                showManual = null
            }
        )
    }

    showType?.let { log ->
        TypeDialog(
            initial = log.type,
            onDismiss = { showType = null },
            onSave = { type ->
                viewModel.tagLog(log.id, type)
                showType = null
            }
        )
    }

    if (showLearn) {
        LearnDialog(
            onDismiss = { showLearn = false },
            onSave = { keyword, kind ->
                if (kind == LearnedPattern.COMPRA) viewModel.learnCompra(keyword)
                else viewModel.learnIgnorar(keyword)
                showLearn = false
            }
        )
    }
}