package com.example.gastos.ui.monthly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import java.text.SimpleDateFormat
import java.util.Locale

private val monthLabelFormat = SimpleDateFormat("MMM", Locale("es", "MX"))
private val monthParseFormat = SimpleDateFormat("yyyy-MM", Locale.US)

private fun monthShortLabel(month: String): String =
    try {
        monthLabelFormat.format(monthParseFormat.parse(month)!!).uppercase(Locale.ROOT)
    } catch (_: Exception) {
        month
    }

@Composable
fun MonthlyBarChart(
    months: List<String>,
    banks: List<String>,
    totalsByMonthAndBank: Map<String, Map<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (months.isEmpty() || banks.isEmpty()) {
        Text(
            text = "Sin datos todavía",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(months, banks, totalsByMonthAndBank) {
        modelProducer.runTransaction {
            columnSeries {
                banks.forEach { bank ->
                    series(
                        x = months.indices.toList(),
                        y = months.map { month -> totalsByMonthAndBank[month]?.get(bank) ?: 0.0 }
                    )
                }
            }
        }
    }

    val monthFormatter = remember(months) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            if (index in months.indices) monthShortLabel(months[index]) else ""
        }
    }

    val bankComponents = banks.map { bank ->
        rememberLineComponent(fill = fill(bankColor(bank)), thickness = 22.dp)
    }
    val columnProvider = remember(bankComponents) {
        ColumnCartesianLayer.ColumnProvider.series(bankComponents)
    }
    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = columnProvider,
        mergeMode = { ColumnCartesianLayer.MergeMode.Stacked }
    )
    val startAxis = VerticalAxis.rememberStart(
        valueFormatter = CartesianValueFormatter.decimal()
    )
    val bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = monthFormatter)
    val chart = rememberCartesianChart(
        layers = arrayOf(columnLayer),
        startAxis = startAxis,
        bottomAxis = bottomAxis
    )

    Column(modifier = modifier.fillMaxWidth()) {
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            banks.forEach { bank ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = bankColor(bank),
                        shape = CircleShape,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Text(
                        text = bank,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            months.forEach { month ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = monthShortLabel(month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}