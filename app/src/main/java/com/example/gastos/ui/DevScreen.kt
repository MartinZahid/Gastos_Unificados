package com.example.gastos.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gastos.R
import com.example.gastos.data.LearnedPattern
import com.example.gastos.data.NotificationLog
import com.example.gastos.engine.NotificationParser
import com.example.gastos.engine.ParseResult
import com.example.gastos.engine.deriveIgnoreKeyword
import com.example.gastos.engine.deriveTrigger
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fixedTypes = listOf("Transferencia", "Saldo", "Promoción", "Retiro", "Otro")

private val testCorpus = listOf(
    "Retiro/Compra COSTCO HERMOSILLO HER COSTCO BANAMEX512 monto \$110.00 el 12/08/26 04:15:13 PM. Auto. 792877",
    "Compra aprobada en OXXO por \$85.50 con tarjeta BANAMEX512",
    "Compraste en NETFLIX \$239.00 con tu tarjeta NU",
    "Pago autorizado en SORIANA \$1,250.00",
    "Comercio WALMART SUPERMERCADO monto \$999.99",
    "Compra realizada en Uber \$60.00"
)

@Composable
fun DevScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.notificationLogs.collectAsStateWithLifecycle()
    val learned by viewModel.learnedPatterns.collectAsStateWithLifecycle()
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

@Composable
private fun DevActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = Volt, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DevLabel(text: String) {
    Text(
        text = text,
        color = Volt,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun LearnedSection(
    patterns: List<LearnedPattern>,
    onDelete: (Long) -> Unit,
    onAdd: () -> Unit
) {
    Column {
        if (patterns.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CardElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sin frases. Se aprenden al marcar Compra/Ignorar en el log.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "Aprender", tint = Volt)
                    }
                }
            }
        } else {
            patterns.take(6).forEach { pattern ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CardElevated
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (pattern.kind == LearnedPattern.COMPRA) "COMPRA" else "IGNORAR",
                            color = if (pattern.kind == LearnedPattern.COMPRA) Volt else Coral,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            pattern.keyword,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { onDelete(pattern.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Borrar",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (patterns.size > 6) {
                    Text(
                        "+${patterns.size - 6} más",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onAdd) {
                    Text("+ Aprender", color = Volt, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LogRow(
    log: NotificationLog,
    onCompra: () -> Unit,
    onIgnorar: () -> Unit,
    onTipo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardElevated
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (log.parsed) Volt else Coral.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (log.parsed) "OK" else "NO",
                        color = if (log.parsed) Ink else Coral,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = log.bank ?: log.packageName,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (log.type != null) {
                    Surface(shape = RoundedCornerShape(6.dp), color = CardBackground) {
                        Text(
                            log.type,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = formatTime(log.dateMillis),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(6.dp))

            if (log.parsed) {
                Text(
                    text = "${log.merchant} · ${formatMoney(log.amount ?: 0.0)}",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    log.text,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    log.text,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "→ ${log.reason ?: "error"}",
                    color = Coral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallAction("Compra", Ink, onCompra, filled = true)
                Spacer(Modifier.width(8.dp))
                SmallAction("Ignorar", Coral, onIgnorar)
                Spacer(Modifier.width(8.dp))
                SmallAction("Tipo", TextPrimary, onTipo, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmallAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) Volt else CardBackground,
        border = if (filled) null else BorderStroke(1.dp, BorderLine)
    ) {
        Text(
            label,
            color = if (filled) Ink else color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ParserTesterDialog(
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
private fun ManualPurchaseDialog(
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
                if (merchant.isNotBlank() && amountText.toDoubleOrNull()?.let { it > 0 } == true) {
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
private fun TypeDialog(
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
private fun LearnDialog(
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

private fun postTestNotification(context: Context, text: String) {
    val channelId = "dev_test"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Pruebas",
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Gastos · Prueba")
        .setContentText(text)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        notification
    )
}

private fun guessAmount(text: String): String {
    val regex = Regex("""\$([\d.,]+)""")
    return regex.find(text)?.groupValues?.get(1) ?: ""
}

private fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM HH:mm", Locale("es", "MX"))
    return formatter.format(Date(millis))
}