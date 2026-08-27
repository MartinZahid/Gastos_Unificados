package com.example.gastos.ui.monthly

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gastos.data.BankMonthSummary
import com.example.gastos.data.Transaction
import com.example.gastos.ui.TransactionViewModel
import com.example.gastos.ui.common.formatMoney
import com.example.gastos.ui.home.TransactionRow
import com.example.gastos.ui.theme.CardBackground
import com.example.gastos.ui.theme.CardElevated
import com.example.gastos.ui.theme.DarkBackground
import com.example.gastos.ui.theme.TextPrimary
import com.example.gastos.ui.theme.TextSecondary
import com.example.gastos.ui.theme.Volt

@Composable
fun MonthlyHistoryScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val recentMonths by viewModel.recentMonths.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val bankSummary by viewModel.bankSummaryForMonth.collectAsStateWithLifecycle()
    val selectedBank by viewModel.selectedHistoryBank.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsByMonthAndBank.collectAsStateWithLifecycle()
    val monthlyByBank by viewModel.monthlyByBank.collectAsStateWithLifecycle()

    val chartData = remember(monthlyByBank) {
        recentMonths.associateWith { month ->
            monthlyByBank[month] ?: emptyMap()
        }
    }
    val chartBanks = remember(chartData) {
        chartData.values.flatMap { it.keys }.distinct().sorted()
    }
    val monthTotal = bankSummary.sumOf { it.total }
    var selectedBankLocal by remember { mutableStateOf<String?>(null) }

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextPrimary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "HISTÓRICO",
                    color = Volt,
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Mes a mes",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        MonthSelector(
            months = recentMonths,
            selected = selectedMonth,
            onSelected = { viewModel.selectMonth(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .background(CardBackground, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "ÚLTIMOS 6 MESES · POR BANCO",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(12.dp))
            MonthlyBarChart(
                months = recentMonths,
                banks = chartBanks,
                totalsByMonthAndBank = chartData
            )
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1.4f)
                .verticalScroll(rememberScrollState())
        ) {
            if (bankSummary.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardBackground
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin gastos registrados en este mes.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bankSummary.forEach { summary ->
                        BankSummaryCard(
                            summary = summary,
                            totalOfAll = monthTotal,
                            isSelected = selectedBankLocal == summary.bank,
                            onClick = {
                                selectedBankLocal =
                                    if (selectedBankLocal == summary.bank) null else summary.bank
                                viewModel.selectHistoryBank(selectedBankLocal)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (transactions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardBackground
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MOVIMIENTOS",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TransactionsCountLabel(count = transactions.size)
                        }
                        Spacer(Modifier.height(10.dp))
                        transactions.forEach { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                onClick = {}
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsCountLabel(count: Int) {
    Surface(shape = RoundedCornerShape(50), color = CardElevated) {
        Text(
            text = count.toString(),
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}