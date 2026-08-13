package com.example.gastos.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastos.data.Transaction
import com.example.gastos.ui.common.formatDate
import com.example.gastos.ui.common.formatMoney
import com.example.gastos.ui.theme.CardElevated
import com.example.gastos.ui.theme.Coral
import com.example.gastos.ui.theme.TextPrimary
import com.example.gastos.ui.theme.TextSecondary

@Composable
internal fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
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