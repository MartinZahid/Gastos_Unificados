package com.example.gastos.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gastos.data.AppDatabase
import com.example.gastos.data.LearnedPattern
import com.example.gastos.data.LearnedPatternDao
import com.example.gastos.data.NotificationLog
import com.example.gastos.data.NotificationLogDao
import com.example.gastos.data.Transaction
import com.example.gastos.data.TransactionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class BankShare(val bank: String, val total: Double)

data class Summary(
    val total: Double = 0.0,
    val count: Int = 0,
    val average: Double = 0.0,
    val max: Double = 0.0,
    val thisMonth: Double = 0.0,
    val byBank: List<BankShare> = emptyList()
)

class TransactionViewModel(
    private val dao: TransactionDao,
    private val logDao: NotificationLogDao,
    private val learnedDao: LearnedPatternDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedBank = MutableStateFlow<String?>(null)
    val selectedBank: StateFlow<String?> = _selectedBank.asStateFlow()

    private val all: StateFlow<List<Transaction>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(all, _query, _selectedBank) { list, query, bank ->
            list.filter { tx ->
                (query.isBlank() || tx.merchant.contains(query, ignoreCase = true)) &&
                    (bank == null || tx.bank == bank)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val banks: StateFlow<List<String>> = all
        .map { list -> list.map { it.bank }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<Summary> = filteredTransactions
        .map { list -> buildSummary(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Summary())

    val notificationLogs: StateFlow<List<NotificationLog>> = logDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val learnedPatterns: StateFlow<List<LearnedPattern>> = learnedDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun selectBank(value: String?) {
        _selectedBank.value = value
    }

    fun simulateTransaction() {
        val merchants = listOf(
            "Supermercado", "Cinepolis", "OXXO", "Netflix", "Uber",
            "Starbucks", "Liverpool", "Farmacia del Ahorro"
        )
        val banks = listOf("BBVA", "Citibanamex", "Santander", "Banorte")
        viewModelScope.launch {
            dao.insert(
                Transaction(
                    merchant = merchants.random(),
                    bank = banks.random(),
                    amount = (50..5000).random() / 100.0,
                    dateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun save(transaction: Transaction) {
        viewModelScope.launch { dao.update(transaction) }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    fun insertPurchase(merchant: String, amount: Double, bank: String) {
        if (merchant.isBlank() || amount <= 0) return
        viewModelScope.launch {
            dao.insert(
                Transaction(
                    merchant = merchant.trim(),
                    amount = amount,
                    bank = bank.ifBlank { "Otro" },
                    dateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun insertManualPurchase(merchant: String, amountText: String, bank: String) {
        val amount = amountText.replace(",", "").replace("$", "").toDoubleOrNull() ?: return
        if (merchant.isBlank() || amount <= 0 || bank.isBlank()) return
        insertPurchase(merchant, amount, bank)
    }

    fun tagLog(id: Long, type: String) {
        viewModelScope.launch { logDao.setType(id, type) }
    }

    fun clearLogs() {
        viewModelScope.launch { logDao.clear() }
    }

    fun deleteLearned(id: Long) {
        viewModelScope.launch { learnedDao.delete(id) }
    }

    fun learnCompra(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            learnedDao.insert(LearnedPattern(keyword = keyword, kind = LearnedPattern.COMPRA))
        }
    }

    fun learnIgnorar(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            learnedDao.insert(LearnedPattern(keyword = keyword, kind = LearnedPattern.IGNORAR))
        }
    }

    private fun buildSummary(list: List<Transaction>): Summary {
        val count = list.size
        val total = list.sumOf { it.amount }
        val average = if (count == 0) 0.0 else total / count
        val max = list.maxOfOrNull { it.amount } ?: 0.0
        val thisMonth = list.filter { isSameMonth(it.dateMillis) }.sumOf { it.amount }
        val byBank = list
            .groupBy { it.bank }
            .map { (bank, txs) -> BankShare(bank, txs.sumOf { it.amount }) }
            .sortedByDescending { it.total }
        return Summary(total, count, average, max, thisMonth, byBank)
    }

    private fun isSameMonth(millis: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        return c.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                val db = AppDatabase.getInstance(app)
                TransactionViewModel(db.transactionDao(), db.notificationLogDao(), db.learnedPatternDao())
            }
        }
    }
}

private val triggerStopWords = setOf(
    "de", "del", "la", "el", "los", "las", "en", "por", "con", "a", "para", "tu", "su",
    "un", "una", "y", "o", "al", "que", "se", "es", "fue", "del", "notificacion",
    "banco", "tarjeta", "tdc", "credito", "crédito", "importante", "aviso", "mensaje",
    "cliente", "hola", "estimado", "estimada", "sr", "sra"
)

fun deriveTrigger(text: String, merchant: String): String? {
    val mFirst = merchant.split(Regex("\\s+")).firstOrNull()?.trim() ?: return null
    if (mFirst.isBlank()) return null
    val idx = text.indexOf(mFirst, ignoreCase = true)
    if (idx <= 0) return null
    val before = text.substring(0, idx)
    val words = before.split(Regex("\\s+"))
        .map { it.trim().trimEnd(',', ':', ';', '/') }
        .filter { it.isNotBlank() }
    val meaningful = words.filter { it.length >= 4 && it.lowercase() !in triggerStopWords }
    return when {
        meaningful.size >= 2 -> meaningful.takeLast(2).joinToString(" ").lowercase()
        meaningful.size == 1 -> meaningful.last().lowercase()
        else -> null
    }
}

private val purchaseMarkers = listOf(
    "compra", "pago", "pagaste", "compraste", "autoriz", "aprob", "cargo",
    "retiro/compra", "establecimiento"
)

fun deriveIgnoreKeyword(text: String): String? {
    if (purchaseMarkers.any { text.contains(it, ignoreCase = true) }) return null
    val first = text.split(Regex("\\s+"))
        .firstOrNull { it.length >= 4 && it.all { c -> c.isLetter() } }
        ?: return null
    return first.lowercase()
}