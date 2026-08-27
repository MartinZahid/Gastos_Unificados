package com.example.gastos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val bank: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val month: String = computeMonth(dateMillis)
) {
    companion object {
        private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)

        fun computeMonth(millis: Long): String = monthFormat.format(Date(millis))
    }
}