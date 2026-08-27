package com.example.gastos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId

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
        // java.time es thread-safe, a diferencia de SimpleDateFormat: las
        // transacciones se crean desde corrutinas concurrentes (listener en
        // Dispatchers.IO) y un formateador compartido corrompería el mes.
        fun computeMonth(millis: Long): String =
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .let { "%04d-%02d".format(it.year, it.monthValue) }
    }
}