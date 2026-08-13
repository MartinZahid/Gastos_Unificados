package com.example.gastos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learned_patterns")
data class LearnedPattern(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val kind: String,
    val dateMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val COMPRA = "COMPRA"
        const val IGNORAR = "IGNORAR"
    }
}