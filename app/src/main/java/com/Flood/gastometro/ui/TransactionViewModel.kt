package com.Flood.gastometro.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Context
import com.Flood.gastometro.data.AppDatabase
import com.Flood.gastometro.data.BankMonthSummary
import com.Flood.gastometro.data.LearnedPattern
import com.Flood.gastometro.data.LearnedPatternDao
import com.Flood.gastometro.data.NotificationLog
import com.Flood.gastometro.data.NotificationLogDao
import com.Flood.gastometro.data.Transaction
import com.Flood.gastometro.data.TransactionDao
import com.Flood.gastometro.listener.BankNotificationListener
import com.Flood.gastometro.ui.common.parseAmountInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val app: Application,
    private val dao: TransactionDao,
    private val logDao: NotificationLogDao,
    private val learnedDao: LearnedPatternDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedBank = MutableStateFlow<String?>(null)
    val selectedBank: StateFlow<String?> = _selectedBank.asStateFlow()

    private val all: StateFlow<List<Transaction>> = dao.observeAll()
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(all, _query, _selectedBank) { list, query, bank ->
            list.filter { tx ->
                (query.isBlank() || tx.merchant.contains(query, ignoreCase = true)) &&
                    (bank == null || tx.bank == bank)
            }
        }.stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    // --- Home orientada al mes ---
    // La pantalla principal muestra el gasto de un mes seleccionado (por
    // defecto el actual) en vez del total acumulado de todos los tiempos.
    private val _homeMonth = MutableStateFlow(Transaction.computeMonth(System.currentTimeMillis()))
    val homeMonth: StateFlow<String> = _homeMonth.asStateFlow()

    val homeTransactions: StateFlow<List<Transaction>> =
        combine(_homeMonth, _query, _selectedBank) { month, query, bank -> Triple(month, query, bank) }
            .flatMapLatest { (month, query, bank) ->
                dao.observeByMonth(month).map { list ->
                    list.filter { tx ->
                        (query.isBlank() || tx.merchant.contains(query, ignoreCase = true)) &&
                            (bank == null || tx.bank == bank)
                    }
                }
            }
            .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    val homeSummary: StateFlow<Summary> = homeTransactions
        .map { list -> buildSummary(list) }
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, Summary())

    fun selectHomeMonth(month: String) {
        _homeMonth.value = month
    }

    val banks: StateFlow<List<String>> = all
        .map { list -> list.map { it.bank }.distinct().sorted() }
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    val summary: StateFlow<Summary> = filteredTransactions
        .map { list -> buildSummary(list) }
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, Summary())

    val notificationLogs: StateFlow<List<NotificationLog>> = logDao.observeAll()
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    // Las notificaciones sin reconocer de bancos soportados (las que cuenta
    // el aviso proactivo), para poder verlas y revisarlas en Modo dev en vez
    // de solo recibir el total en un aviso.
    val unreviewedFailures: StateFlow<List<NotificationLog>> = logDao.observeUnreviewedFailures()
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    val learnedPatterns: StateFlow<List<LearnedPattern>> = learnedDao.observeAll()
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    // Notificaciones bancarias que el parser no pudo leer y aún no se
    // revisan. Alimenta el aviso proactivo (badge en el drawer + banner en
    // Modo dev) para no perder gastos silenciosamente.
    val unreviewedFailureCount: StateFlow<Int> = logDao.observeUnreviewedFailureCount()
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, 0)

    // --- Histórico mensual por banco ---

    val recentMonths: StateFlow<List<String>> = dao.observeRecentMonths(6)
        .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    // Totales agrupados por mes y banco, para la gráfica de barras apiladas.
    val monthlyByBank: StateFlow<Map<String, Map<String, Double>>> =
        dao.observeMonthlyByBank()
            .map { rows ->
                rows.groupBy({ it.month }, { it.bank to it.total })
                    .mapValues { (_, pairs) -> pairs.toMap() }
            }
            .stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyMap())

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    init {
        viewModelScope.launch {
            recentMonths.collect { months ->
                if (_selectedMonth.value.isBlank() && months.isNotEmpty()) {
                    _selectedMonth.value = months.first()
                }
            }
        }
    }

    val bankSummaryForMonth: StateFlow<List<BankMonthSummary>> =
        _selectedMonth.flatMapLatest { month ->
            if (month.isBlank()) flowOf(emptyList())
            else dao.observeByBankAndMonth(month)
        }.stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    private val _selectedHistoryBank = MutableStateFlow<String?>(null)
    val selectedHistoryBank: StateFlow<String?> = _selectedHistoryBank.asStateFlow()

    val transactionsByMonthAndBank: StateFlow<List<Transaction>> =
        combine(_selectedMonth, _selectedHistoryBank) { month, bank -> month to bank }
            .flatMapLatest { (month, bank) ->
                if (month.isBlank()) flowOf(emptyList())
                else if (bank.isNullOrBlank()) dao.observeByMonth(month)
                else dao.observeByMonthAndBank(month, bank)
            }.stateIn(viewModelScope, STOP_SHARING_TIMEOUT, emptyList())

    fun selectMonth(month: String) {
        _selectedMonth.value = month
        _selectedHistoryBank.value = null
    }

    fun selectHistoryBank(bank: String?) {
        _selectedHistoryBank.value = bank
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun selectBank(value: String?) {
        _selectedBank.value = value
    }

    fun save(transaction: Transaction) {
        viewModelScope.launch { dao.update(transaction) }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    // Inserta una compra ya validada (desde el log dev parseado).
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

    // Inserta una compra escrita a mano en el formulario dev (texto crudo).
    fun insertManualPurchase(merchant: String, amountText: String, bank: String) {
        val amount = parseAmountInput(amountText) ?: return
        if (merchant.isBlank() || amount <= 0 || bank.isBlank()) return
        insertPurchase(merchant, amount, bank)
    }

    fun tagLog(id: Long, type: String) {
        viewModelScope.launch { logDao.setType(id, type) }
    }

    fun clearLogs() {
        viewModelScope.launch { logDao.clear() }
    }

    // El usuario ya vio el log de "no reconocidas" en Modo dev: apaga la
    // alerta hasta que llegue una nueva notificación sin parsear.
    // Además limpia el enfriamiento para que un problema distinto
    // pueda avisar de inmediato sin esperar 12 h.
    fun markFailuresReviewed() {
        viewModelScope.launch {
            logDao.markFailuresReviewed()
            app.getSharedPreferences(BankNotificationListener.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(BankNotificationListener.PREF_LAST_ALERT_MILLIS)
                .apply()
        }
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
        // Tiempo que un flujo sigue emitiendo tras quedarse sin suscriptores.
        private val STOP_SHARING_TIMEOUT = SharingStarted.WhileSubscribed(5_000)

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                val db = AppDatabase.getInstance(app)
                TransactionViewModel(app, db.transactionDao(), db.notificationLogDao(), db.learnedPatternDao())
            }
        }
    }
}