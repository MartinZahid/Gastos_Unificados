package com.Flood.gastometro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class BankMonthSummary(val bank: String, val total: Double, val count: Int)
data class BankMonthlyTotal(val month: String, val bank: String, val total: Double)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions")
    fun observeTotal(): Flow<Double>

    @Query("SELECT bank, SUM(amount) as total, COUNT(*) as count FROM transactions WHERE month = :month GROUP BY bank ORDER BY total DESC")
    fun observeByBankAndMonth(month: String): Flow<List<BankMonthSummary>>

    @Query("SELECT month, bank, SUM(amount) as total FROM transactions GROUP BY month, bank ORDER BY month DESC")
    fun observeMonthlyByBank(): Flow<List<BankMonthlyTotal>>

    @Query("SELECT DISTINCT month FROM transactions WHERE month != '' ORDER BY month DESC LIMIT :limit")
    fun observeRecentMonths(limit: Int = 6): Flow<List<String>>

    @Query("SELECT * FROM transactions WHERE month = :month ORDER BY dateMillis DESC")
    fun observeByMonth(month: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE month = :month AND bank = :bank ORDER BY dateMillis DESC")
    fun observeByMonthAndBank(month: String, bank: String): Flow<List<Transaction>>
}
