package com.Flood.gastometro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Flood.gastometro.ui.Summary
import com.Flood.gastometro.ui.common.CornerCutShape
import com.Flood.gastometro.ui.common.formatMoney
import com.Flood.gastometro.ui.theme.CardBackground
import com.Flood.gastometro.ui.theme.Ink
import com.Flood.gastometro.ui.theme.TextSecondary
import com.Flood.gastometro.ui.theme.Volt

// Tarjeta principal con el total gastado (fondo volt).
@Composable
internal fun HeroCard(summary: Summary) {
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

// Metrica compacta (este mes, mayor gasto) bajo la tarjeta principal.
@Composable
internal fun MetricTile(
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