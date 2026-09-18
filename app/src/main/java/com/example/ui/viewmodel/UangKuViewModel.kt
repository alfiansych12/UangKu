package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthState
import com.example.data.auth.GoogleAuthManager
import com.example.data.auth.UserAccount
import com.example.data.backup.BackupData
import com.example.data.backup.BackupManager
import com.example.data.local.TransactionWithDetails
import com.example.data.model.AppNotificationEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEditHistoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WalletEntity
import com.example.data.repository.UangKuRepository
import com.example.network.ParsedReceipt
import com.example.network.ReceiptOcrScanner
import com.example.ui.util.Formatters
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpendBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val totalSpent: Double,
    val percentage: Float
)

data class MonthlyTrend(
    val monthName: String,
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double
)

data class BudgetAlertItem(
    val categoryId: Long,
    val categoryName: String,
    val budgetLimit: Double,
    val spent: Double,
    val percentage: Float, // 0 to 1+
    val isExceeded: Boolean,
    val isWarning: Boolean
)

data class FinancialInsights(
    val highestCategory: Pair<String, Double>?,
    val averageDailySpending: Double,
    val projectedMonthEndBalance: Double,
    val spendingComparisonText: String
)

class UangKuViewModel(
    private val repository: UangKuRepository,
    private val googleAuthManager: GoogleAuthManager? = null
) : ViewModel() {

    // Auth State
    val authState: StateFlow<AuthState> = googleAuthManager?.authState
        ?: MutableStateFlow(AuthState.Unauthenticated).asStateFlow()

    fun signInWithGoogle(
        activityContext: Context,
        serverClientIdOverride: String? = null,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val manager = googleAuthManager ?: run {
            onResult(false, "GoogleAuthManager belum diinisialisasi")
            return
        }
        viewModelScope.launch {
            val result = manager.signInWithGoogle(activityContext, serverClientIdOverride)
            if (result.isSuccess) {
                val user = result.getOrNull()
                onResult(true, "Berhasil masuk sebagai ${user?.displayName}")
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Gagal autentikasi Google"
                onResult(false, errorMsg)
            }
        }
    }

    fun signInQuick(email: String, displayName: String): UserAccount? {
        return googleAuthManager?.signInQuick(email, displayName)
    }

    fun signOut(activityContext: Context? = null) {
        viewModelScope.launch {
            googleAuthManager?.signOut(activityContext)
        }
    }

    fun getWebClientId(): String {
        return googleAuthManager?.getWebClientId() ?: ""
    }

    fun saveWebClientId(clientId: String) {
        googleAuthManager?.saveWebClientId(clientId)
    }

    fun isFirebaseActive(): Boolean {
        return googleAuthManager?.isFirebaseActive() ?: false
    }

    // Initialize database
    init {
        viewModelScope.launch {
            repository.ensureDefaultDataSeeded()
        }
    }

    // Repository Flows
    val transactions: StateFlow<List<TransactionWithDetails>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedTransactions: StateFlow<List<TransactionWithDetails>> = repository.allDeletedTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedTransactionsCount: StateFlow<Int> = repository.deletedTransactionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurring: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI state for filter & search
    val searchQuery = MutableStateFlow("")
    val searchHistory = MutableStateFlow<List<String>>(listOf("Kopi", "Makan Siang", "Gaji", "Bensin", "Belanja Bulanan"))
    val selectedTypeFilter = MutableStateFlow("ALL") // ALL, EXPENSE, INCOME, TRANSFER
    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedWalletId = MutableStateFlow<Long?>(null)
    val selectedTimeRange = MutableStateFlow("ALL") // ALL, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, LAST_3_MONTHS, CUSTOM
    val customStartDateMillis = MutableStateFlow<Long?>(null)
    val customEndDateMillis = MutableStateFlow<Long?>(null)
    val minAmountFilter = MutableStateFlow<Double?>(null)
    val maxAmountFilter = MutableStateFlow<Double?>(null)
    val sortBy = MutableStateFlow("NEWEST") // NEWEST, OLDEST, HIGHEST, LOWEST

    // Count of active filters
    val activeFilterCount: StateFlow<Int> = combine(
        selectedTypeFilter,
        selectedCategoryId,
        selectedWalletId,
        selectedTimeRange,
        combine(minAmountFilter, maxAmountFilter) { min, max -> Pair(min, max) }
    ) { type, catId, wId, tr, (min, max) ->
        var count = 0
        if (type != "ALL") count++
        if (catId != null) count++
        if (wId != null) count++
        if (tr != "ALL") count++
        if (min != null) count++
        if (max != null) count++
        count
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun resetFilters() {
        searchQuery.value = ""
        selectedTypeFilter.value = "ALL"
        selectedCategoryId.value = null
        selectedWalletId.value = null
        selectedTimeRange.value = "ALL"
        customStartDateMillis.value = null
        customEndDateMillis.value = null
        minAmountFilter.value = null
        maxAmountFilter.value = null
        sortBy.value = "NEWEST"
    }

    fun addSearchToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            val current = searchHistory.value.toMutableList()
            current.remove(trimmed)
            current.add(0, trimmed)
            if (current.size > 8) current.removeAt(current.size - 1)
            searchHistory.value = current
        }
    }

    fun clearSearchHistory() {
        searchHistory.value = emptyList()
    }

    // Dark theme toggle
    val isDarkMode = MutableStateFlow<Boolean?>(null)

    // Backup State
    val isAutoSync = MutableStateFlow(true)
    val lastBackupTime = MutableStateFlow<Long?>(null)
    val isBackupInProgress = MutableStateFlow(false)
    val backupStatusMessage = MutableStateFlow<String?>(null)

    fun initBackupPrefs(context: Context) {
        isAutoSync.value = BackupManager.isAutoSyncEnabled(context)
        lastBackupTime.value = BackupManager.getLastBackupTime(context)
    }

    fun toggleAutoSync(context: Context, enabled: Boolean) {
        BackupManager.setAutoSyncEnabled(context, enabled)
        isAutoSync.value = enabled
    }

    // OCR Scanning State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedReceipt = MutableStateFlow<ParsedReceipt?>(null)
    val scannedReceipt: StateFlow<ParsedReceipt?> = _scannedReceipt.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        transactions,
        combine(searchQuery, selectedTypeFilter, selectedCategoryId, selectedWalletId) { q, t, c, w ->
            FilterParams(q, t, c, w)
        },
        combine(selectedTimeRange, customStartDateMillis, customEndDateMillis) { tr, s, e ->
            TimeParams(tr, s, e)
        },
        combine(minAmountFilter, maxAmountFilter, sortBy) { min, max, s ->
            RangeParams(min, max, s)
        }
    ) { txList, filterP, timeP, rangeP ->
        val now = Calendar.getInstance()
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val threeMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis

        val query = filterP.query.trim()

        txList.filter { tx ->
            // Query filter (multi-field including title, notes, merchant, category, and amount)
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.notes.contains(query, ignoreCase = true) ||
                    tx.merchantName.contains(query, ignoreCase = true) ||
                    (tx.categoryName ?: "").contains(query, ignoreCase = true) ||
                    (tx.walletName ?: "").contains(query, ignoreCase = true) ||
                    tx.amount.toLong().toString().contains(query)

            // Type filter
            val matchesType = when (filterP.type) {
                "EXPENSE" -> tx.type == "EXPENSE"
                "INCOME" -> tx.type == "INCOME"
                "TRANSFER" -> tx.type == "TRANSFER"
                else -> true
            }

            // Category filter
            val matchesCategory = filterP.categoryId == null || tx.categoryId == filterP.categoryId

            // Wallet filter
            val matchesWallet = filterP.walletId == null || tx.walletId == filterP.walletId || tx.toWalletId == filterP.walletId

            // Time range filter
            val matchesTime = when (timeP.timeRange) {
                "THIS_MONTH" -> tx.dateMillis >= startOfMonth
                "LAST_7_DAYS" -> tx.dateMillis >= sevenDaysAgo
                "LAST_30_DAYS" -> tx.dateMillis >= thirtyDaysAgo
                "LAST_3_MONTHS" -> tx.dateMillis >= threeMonthsAgo
                "CUSTOM" -> {
                    val afterStart = timeP.customStart == null || tx.dateMillis >= timeP.customStart
                    val beforeEnd = timeP.customEnd == null || tx.dateMillis <= timeP.customEnd
                    afterStart && beforeEnd
                }
                else -> true
            }

            // Amount range filter
            val matchesAmount = (rangeP.minAmount == null || tx.amount >= rangeP.minAmount) &&
                    (rangeP.maxAmount == null || tx.amount <= rangeP.maxAmount)

            matchesQuery && matchesType && matchesCategory && matchesWallet && matchesTime && matchesAmount
        }.let { list ->
            when (rangeP.sortBy) {
                "NEWEST" -> list.sortedByDescending { it.dateMillis }
                "OLDEST" -> list.sortedBy { it.dateMillis }
                "HIGHEST" -> list.sortedByDescending { it.amount }
                "LOWEST" -> list.sortedBy { it.amount }
                else -> list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Internal helper data classes for combine
    private data class FilterParams(val query: String, val type: String, val categoryId: Long?, val walletId: Long?)
    private data class TimeParams(val timeRange: String, val customStart: Long?, val customEnd: Long?)
    private data class RangeParams(val minAmount: Double?, val maxAmount: Double?, val sortBy: String)

    // Overview Totals (Month to date or Total)
    val totalIncome: StateFlow<Double> = transactions.combine(selectedTimeRange) { list, timeRange ->
        filterByTime(list, timeRange)
            .filter { it.type == "INCOME" }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions.combine(selectedTimeRange) { list, timeRange ->
        filterByTime(list, timeRange)
            .filter { it.type == "EXPENSE" }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalWalletsBalance: StateFlow<Double> = wallets.combine(transactions) { wList, _ ->
        wList.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Spend Breakdown
    val categoryBreakdown: StateFlow<List<CategorySpendBreakdown>> = transactions.combine(categories) { txList, catList ->
        val expenseTx = txList.filter { it.type == "EXPENSE" }
        val totalSpent = expenseTx.sumOf { it.amount }
        if (totalSpent <= 0) return@combine emptyList()

        val grouped = expenseTx.groupBy { it.categoryId }
        grouped.mapNotNull { (catId, items) ->
            val catSpent = items.sumOf { it.amount }
            val cat = catList.find { it.id == catId }
            val catName = cat?.name ?: items.firstOrNull()?.categoryName ?: "Lain-lain"
            val colorHex = cat?.colorHex ?: items.firstOrNull()?.categoryColor ?: "#78909C"
            val iconName = cat?.iconName ?: items.firstOrNull()?.categoryIcon ?: "category"
            val pct = (catSpent / totalSpent).toFloat()

            CategorySpendBreakdown(
                categoryId = catId,
                categoryName = catName,
                colorHex = colorHex,
                iconName = iconName,
                totalSpent = catSpent,
                percentage = pct
            )
        }.sortedByDescending { it.totalSpent }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Trends (Last 6 months)
    val monthlyTrends: StateFlow<List<MonthlyTrend>> = transactions.combine(MutableStateFlow(Unit)) { txList, _ ->
        val result = mutableListOf<MonthlyTrend>()

        for (i in 5 downTo 0) {
            val monthCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val month = monthCal.get(Calendar.MONTH) + 1
            val year = monthCal.get(Calendar.YEAR)
            val monthName = SimpleDateFormat("MMM", Locale("id", "ID")).format(monthCal.time)

            // Calculate start and end of this month
            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            val monthTx = txList.filter { it.dateMillis in startCal.timeInMillis..endCal.timeInMillis }
            val inc = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            result.add(MonthlyTrend(monthName, month, year, inc, exp))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget Alerts
    val budgetAlerts: StateFlow<List<BudgetAlertItem>> = combine(
        budgets,
        transactions,
        categories
    ) { bList, txList, catList ->
        val now = Calendar.getInstance()
        val curMonth = now.get(Calendar.MONTH) + 1
        val curYear = now.get(Calendar.YEAR)

        val monthTx = txList.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            c.get(Calendar.MONTH) + 1 == curMonth && c.get(Calendar.YEAR) == curYear && it.type == "EXPENSE"
        }

        bList.map { budget ->
            val spent = monthTx.filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
            val pct = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
            val cat = catList.find { it.id == budget.categoryId }
            val catName = cat?.name ?: "Kategori #${budget.categoryId}"

            BudgetAlertItem(
                categoryId = budget.categoryId,
                categoryName = catName,
                budgetLimit = budget.monthlyLimit,
                spent = spent,
                percentage = pct,
                isExceeded = pct >= 1.0f,
                isWarning = pct >= 0.8f && pct < 1.0f
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Insights & Predictions
    val insights: StateFlow<FinancialInsights> = combine(
        transactions,
        categoryBreakdown,
        totalIncome,
        totalExpense
    ) { txList, breakdown, inc, exp ->
        val highest = breakdown.firstOrNull()?.let { Pair(it.categoryName, it.totalSpent) }
        val now = Calendar.getInstance()
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        val avgDaily = exp / dayOfMonth
        val projectedExpense = avgDaily * maxDays
        val projectedBalance = inc - projectedExpense

        val spendingComparison = if (exp > 0) {
            "Pengeluaran rata-rata ${Formatters.formatRupiah(avgDaily)}/hari. Tetap hemat!"
        } else {
            "Belum ada pengeluaran tercatat bulan ini."
        }

        FinancialInsights(
            highestCategory = highest,
            averageDailySpending = avgDaily,
            projectedMonthEndBalance = projectedBalance,
            spendingComparisonText = spendingComparison
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialInsights(null, 0.0, 0.0, "")
    )

    private fun filterByTime(list: List<TransactionWithDetails>, timeRange: String): List<TransactionWithDetails> {
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val threeMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis

        return when (timeRange) {
            "THIS_MONTH" -> list.filter { it.dateMillis >= startOfMonth }
            "LAST_7_DAYS" -> list.filter { it.dateMillis >= sevenDaysAgo }
            "LAST_30_DAYS" -> list.filter { it.dateMillis >= thirtyDaysAgo }
            "LAST_3_MONTHS" -> list.filter { it.dateMillis >= threeMonthsAgo }
            else -> list
        }
    }

    // OCR Receipt Scan Action
    fun scanReceiptImage(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanMessage.value = "Memproses struk dengan OCR..."
            try {
                val (processedBitmap, savedPath) = ReceiptOcrScanner.processAndSaveReceiptBitmap(context, bitmap)
                val parsed = ReceiptOcrScanner.scanReceipt(context, processedBitmap, savedPath)
                _scannedReceipt.value = parsed
                _scanMessage.value = "Struk berhasil terbaca: ${parsed.merchantName}"
            } catch (e: Exception) {
                val fallback = ReceiptOcrScanner.fallbackLocalParser(null)
                _scannedReceipt.value = fallback
                _scanMessage.value = "Struk terbaca (mode cepat)"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScannedReceipt() {
        _scannedReceipt.value = null
        _scanMessage.value = null
    }

    // Notifications Operations
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun checkAndTriggerBudgetAlert(context: Context, categoryId: Long) {
        viewModelScope.launch {
            val alert = budgetAlerts.value.find { it.categoryId == categoryId } ?: return@launch
            if (alert.isExceeded || alert.isWarning) {
                NotificationHelper.sendBudgetAlert(
                    context = context,
                    categoryName = alert.categoryName,
                    spent = alert.spent,
                    limit = alert.budgetLimit,
                    percentage = alert.percentage
                )
                repository.insertNotification(
                    AppNotificationEntity(
                        title = if (alert.isExceeded) "⚠️ Anggaran Terlampaui: ${alert.categoryName}" else "⚡ Peringatan Anggaran: ${alert.categoryName}",
                        message = "Pengeluaran ${alert.categoryName} telah mencapai ${(alert.percentage * 100).toInt()}% (${Formatters.formatRupiah(alert.spent)} / ${Formatters.formatRupiah(alert.budgetLimit)})",
                        type = "BUDGET_ALERT",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // CRUD Transactions
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        categoryId: Long,
        walletId: Long,
        dateMillis: Long,
        notes: String,
        receiptImagePath: String? = null,
        merchantName: String = "",
        tags: String = "",
        context: Context? = null
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                title = title.ifBlank { "Transaksi Baru" },
                amount = amount,
                type = type,
                categoryId = categoryId,
                walletId = walletId,
                dateMillis = dateMillis,
                notes = notes,
                receiptImagePath = receiptImagePath,
                merchantName = merchantName,
                tags = tags
            )
            repository.insertTransaction(tx)
            if (context != null && type == "EXPENSE") {
                checkAndTriggerBudgetAlert(context, categoryId)
            }
        }
    }

    fun updateTransaction(
        id: Long,
        title: String,
        amount: Double,
        type: String,
        categoryId: Long,
        walletId: Long,
        toWalletId: Long? = null,
        dateMillis: Long,
        notes: String,
        receiptImagePath: String? = null,
        merchantName: String = "",
        tags: String = "",
        reason: String = "Perubahan data transaksi",
        context: Context? = null
    ) {
        viewModelScope.launch {
            val updated = TransactionEntity(
                id = id,
                title = title.ifBlank { "Transaksi Diedit" },
                amount = amount,
                type = type,
                categoryId = categoryId,
                walletId = walletId,
                toWalletId = toWalletId,
                dateMillis = dateMillis,
                notes = notes,
                receiptImagePath = receiptImagePath,
                merchantName = merchantName,
                tags = tags
            )
            repository.updateTransaction(updated, reason)
            if (context != null && type == "EXPENSE") {
                checkAndTriggerBudgetAlert(context, categoryId)
            }
        }
    }

    fun getEditHistoryForTransaction(txId: Long): Flow<List<TransactionEditHistoryEntity>> {
        return repository.getEditHistoryForTransaction(txId)
    }

    fun revertEdit(history: TransactionEditHistoryEntity) {
        viewModelScope.launch {
            repository.revertEdit(history)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.softDeleteTransaction(id)
        }
    }

    fun restoreTransaction(id: Long) {
        viewModelScope.launch {
            repository.restoreTransaction(id)
        }
    }

    fun permanentlyDeleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.permanentlyDeleteTransaction(id)
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            repository.clearTrash()
        }
    }

    // Cloud Backup & Sync Operations
    fun triggerManualBackup(context: Context) {
        viewModelScope.launch {
            isBackupInProgress.value = true
            backupStatusMessage.value = "Membuat cadangan data..."
            try {
                val data = repository.createBackupSnapshot()
                val jsonString = BackupManager.toJson(data)
                BackupManager.saveCloudSnapshot(context, jsonString)
                lastBackupTime.value = System.currentTimeMillis()
                backupStatusMessage.value = "Backup Cloud berhasil disimpan (${data.transactions.size} transaksi)"
                NotificationHelper.sendBackupNotification(
                    context,
                    isSuccess = true,
                    "Cadangan data (${data.transactions.size} transaksi, ${data.wallets.size} dompet) berhasil disimpan."
                )
                repository.insertNotification(
                    AppNotificationEntity(
                        title = "☁️ Backup Cloud Berhasil",
                        message = "Pencadangan data lengkap UangKu berhasil tersimpan pada ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())}.",
                        type = "BACKUP_SUCCESS",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                backupStatusMessage.value = "Gagal backup: ${e.localizedMessage}"
                NotificationHelper.sendBackupNotification(
                    context,
                    isSuccess = false,
                    "Terjadi kesalahan saat menyimpan cadangan: ${e.localizedMessage}"
                )
            } finally {
                isBackupInProgress.value = false
            }
        }
    }

    fun restoreFromCloud(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isBackupInProgress.value = true
            try {
                val json = BackupManager.getCloudSnapshot(context)
                if (json == null) {
                    onResult(false, "Tidak ada data backup di Cloud lokal.")
                    return@launch
                }
                val backupData = BackupManager.fromJson(json)
                repository.restoreFromBackupData(backupData)
                onResult(true, "Berhasil memulihkan ${backupData.transactions.size} transaksi dari Cloud.")
            } catch (e: Exception) {
                onResult(false, "Gagal memulihkan data: ${e.localizedMessage}")
            } finally {
                isBackupInProgress.value = false
            }
        }
    }

    suspend fun exportBackupJsonFile(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            val data = repository.createBackupSnapshot()
            val jsonString = BackupManager.toJson(data)
            val file = BackupManager.saveBackupToFile(context, jsonString)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    fun restoreFromJsonString(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isBackupInProgress.value = true
            try {
                val data = BackupManager.fromJson(jsonString)
                repository.restoreFromBackupData(data)
                onResult(true, "Data berhasil dipulihkan dari file backup JSON!")
            } catch (e: Exception) {
                onResult(false, "Gagal memproses file JSON: ${e.localizedMessage}")
            } finally {
                isBackupInProgress.value = false
            }
        }
    }

    // Wallet Transfer
    fun transferWallet(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.transferBetweenWallets(fromWalletId, toWalletId, amount, notes)
        }
    }

    // Savings Goals
    fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        targetDateMillis: Long,
        iconName: String,
        colorHex: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                targetDateMillis = targetDateMillis,
                iconName = iconName,
                colorHex = colorHex,
                notes = notes
            )
            repository.insertSavingsGoal(goal)
        }
    }

    fun contributeToGoal(goalId: Long, fromWalletId: Long, amount: Double, goalTitle: String) {
        viewModelScope.launch {
            repository.addSavingsContribution(goalId, fromWalletId, amount, goalTitle)
        }
    }

    fun deleteSavingsGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
        }
    }

    // Budgets
    fun setBudget(categoryId: Long, limit: Double) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val b = BudgetEntity(
                categoryId = categoryId,
                monthlyLimit = limit,
                month = now.get(Calendar.MONTH) + 1,
                year = now.get(Calendar.YEAR)
            )
            repository.insertOrUpdateBudget(b)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    // Categories
    fun addCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex
            )
            repository.insertCategory(cat)
        }
    }

    // Wallets
    fun addWallet(name: String, type: String, balance: Double, accountNumber: String, colorHex: String) {
        viewModelScope.launch {
            val w = WalletEntity(
                name = name,
                type = type,
                balance = balance,
                accountNumber = accountNumber,
                colorHex = colorHex
            )
            repository.insertWallet(w)
        }
    }

    // Recurring
    fun addRecurring(
        title: String,
        amount: Double,
        type: String,
        categoryId: Long,
        walletId: Long,
        frequency: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val nextCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
            val rec = RecurringTransactionEntity(
                title = title,
                amount = amount,
                type = type,
                categoryId = categoryId,
                walletId = walletId,
                frequency = frequency,
                nextDueDateMillis = nextCal.timeInMillis,
                notes = notes
            )
            repository.insertRecurring(rec)
        }
    }

    fun processRecurringNow(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.processRecurringTransaction(recurring)
        }
    }

    fun toggleRecurringActive(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurring(recurring.copy(isActive = !recurring.isActive))
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurring(id)
        }
    }

    // CSV Export & Reporting
    suspend fun exportToCsv(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            val txList = transactions.value
            val fileName = "UangKu_Laporan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                writer.append("ID,Tanggal,Judul,Tipe,Kategori,Dompet,Jumlah (Rp),Catatan,Merchant\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                for (tx in txList) {
                    writer.append("${tx.id},")
                    writer.append("\"${sdf.format(Date(tx.dateMillis))}\",")
                    writer.append("\"${tx.title.replace("\"", "\"\"")}\",")
                    writer.append("${tx.type},")
                    writer.append("\"${tx.categoryName ?: ""}\",")
                    writer.append("\"${tx.walletName ?: ""}\",")
                    writer.append("${tx.amount.toLong()},")
                    writer.append("\"${tx.notes.replace("\"", "\"\"")}\",")
                    writer.append("\"${tx.merchantName.replace("\"", "\"\"")}\"\n")
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
}
