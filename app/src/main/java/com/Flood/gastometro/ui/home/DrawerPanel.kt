package com.Flood.gastometro.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Flood.gastometro.ui.BankShare
import com.Flood.gastometro.ui.Summary
import com.Flood.gastometro.ui.common.formatMoney
import com.Flood.gastometro.ui.theme.BorderLine
import com.Flood.gastometro.ui.theme.CardElevated
import com.Flood.gastometro.ui.theme.Coral
import com.Flood.gastometro.ui.theme.Ink
import com.Flood.gastometro.ui.theme.TextPrimary
import com.Flood.gastometro.ui.theme.TextSecondary
import com.Flood.gastometro.ui.theme.Volt

// Panel lateral (drawer) con filtros y análisis.
@Composable
internal fun DrawerPanel(
    query: String,
    banks: List<String>,
    selectedBank: String?,
    summary: Summary,
    onQueryChange: (String) -> Unit,
    onBankSelected: (String?) -> Unit,
    onClose: () -> Unit,
    onOpenDev: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenKeepAlive: () -> Unit,
    unreviewedFailureCount: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                // Aviso de que hay notificaciones bancarias sin reconocer:
                // visible desde el drawer, sin tener que entrar a Modo dev
                // para descubrir que se están perdiendo gastos.
                if (unreviewedFailureCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Coral,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = unreviewedFailureCount.toString(),
                            color = Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            onClick = onOpenHistory,
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
                    text = "Histórico",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            onClick = onOpenKeepAlive,
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
                    text = "No dormir la app",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
            shares.take(3).forEachIndexed { index, share ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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