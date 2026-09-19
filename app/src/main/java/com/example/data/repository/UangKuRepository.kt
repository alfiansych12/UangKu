package com.example.data.repository

import com.example.data.backup.BackupData
import com.example.data.local.AppDao
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionWithDetails
import com.example.data.model.AppNotificationEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEditHistoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WalletEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class UangKuRepository(private val dao: AppDao) {
    val allTransactions: Flow<List<TransactionWithDetails>> = dao.getAllTransactionsWithDetails()
    val allDeletedTransactions: Flow<List<TransactionWithDetails>> = dao.getDeletedTransactionsWithDetails()
    val deletedTransactionsCount: Flow<Int> = dao.getDeletedTransactionsCount()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allWallets: Flow<List<WalletEntity>> = dao.getAllWallets()
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    val allRecurring: Flow<List<RecurringTransactionEntity>> = dao.getAllRecurring()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()
    val allNotifications: Flow<List<AppNotificationEntity>> = dao.getAllNotifications()
    val unreadNotificationCount: Flow<Int> = dao.getUnreadNotificationCount()

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionWithDetails>> {
        return dao.getTransactionsBetween(startMillis, endMillis)
    }

    fun getEditHistoryForTransaction(txId: Long): Flow<List<TransactionEditHistoryEntity>> {
        return dao.getEditHistoryForTransaction(txId)
    }

    private suspend fun applyWalletEffect(tx: TransactionEntity) {
        when (tx.type) {
            "EXPENSE" -> {
                dao.updateWalletBalance(tx.walletId, -tx.amount)
            }
            "INCOME" -> {
                dao.updateWalletBalance(tx.walletId, tx.amount)
            }
            "TRANSFER" -> {
                dao.updateWalletBalance(tx.walletId, -tx.amount)
                tx.toWalletId?.let { targetId ->
                    dao.updateWalletBalance(targetId, tx.amount)
                }
            }
        }
    }

    private suspend fun revertWalletEffect(tx: TransactionEntity) {
        when (tx.type) {
            "EXPENSE" -> {
                dao.updateWalletBalance(tx.walletId, tx.amount)
            }
            "INCOME" -> {
                dao.updateWalletBalance(tx.walletId, -tx.amount)
            }
            "TRANSFER" -> {
                dao.updateWalletBalance(tx.walletId, tx.amount)
                tx.toWalletId?.let { targetId ->
                    dao.updateWalletBalance(targetId, -tx.amount)
                }
            }
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = dao.insertTransaction(transaction)
        applyWalletEffect(transaction)
        return id
    }

    suspend fun updateTransaction(
        updatedTx: TransactionEntity,
        reason: String = "Perubahan data transaksi"
    ) {
        val oldTx = dao.getTransactionById(updatedTx.id) ?: return

        // 1. Revert old transaction's wallet balance impact
        revertWalletEffect(oldTx)

        // 2. Record history
        val history = TransactionEditHistoryEntity(
            transactionId = updatedTx.id,
            editTimestamp = System.currentTimeMillis(),
            previousTitle = oldTx.title,
            previousAmount = oldTx.amount,
            previousType = oldTx.type,
            previousCategoryId = oldTx.categoryId,
            previousWalletId = oldTx.walletId,
            previousToWalletId = oldTx.toWalletId,
            previousDateMillis = oldTx.dateMillis,
            previousNotes = oldTx.notes,
            previousMerchantName = oldTx.merchantName,
            newTitle = updatedTx.title,
            newAmount = updatedTx.amount,
            newType = updatedTx.type,
            newCategoryId = updatedTx.categoryId,
            newWalletId = updatedTx.walletId,
            newToWalletId = updatedTx.toWalletId,
            newDateMillis = updatedTx.dateMillis,
            newNotes = updatedTx.notes,
            newMerchantName = updatedTx.merchantName,
            reason = reason
        )
        dao.insertEditHistory(history)

        // 3. Mark as edited and update timestamp
        val txToSave = updatedTx.copy(
            isEdited = true,
            modifiedAt = System.currentTimeMillis()
        )
        dao.updateTransaction(txToSave)

        // 4. Apply new transaction's wallet balance impact
        applyWalletEffect(txToSave)
    }

    suspend fun revertEdit(history: TransactionEditHistoryEntity) {
        val currentTx = dao.getTransactionById(history.transactionId) ?: return

        val revertedTx = currentTx.copy(
            title = history.previousTitle,
            amount = history.previousAmount,
            type = history.previousType,
            categoryId = history.previousCategoryId,
            walletId = history.previousWalletId,
            toWalletId = history.previousToWalletId,
            dateMillis = history.previousDateMillis,
            notes = history.previousNotes,
            merchantName = history.previousMerchantName,
            isEdited = true,
            modifiedAt = System.currentTimeMillis()
        )

        updateTransaction(revertedTx, reason = "Batal edit / Rollback ke versi sebelumnya")
    }

    suspend fun softDeleteTransaction(id: Long) {
        val tx = dao.getTransactionById(id) ?: return
        revertWalletEffect(tx)
        dao.softDeleteTransaction(id, System.currentTimeMillis())
    }

    suspend fun restoreTransaction(id: Long) {
        val tx = dao.getTransactionById(id) ?: return
        applyWalletEffect(tx)
        dao.restoreTransaction(id)
    }

    suspend fun permanentlyDeleteTransaction(id: Long) {
        dao.deleteEditHistoryForTransaction(id)
        dao.deleteTransactionById(id)
    }

    suspend fun clearTrash(): Int {
        return dao.clearTrash()
    }

    suspend fun deleteTransaction(id: Long) {
        softDeleteTransaction(id)
    }

    suspend fun transferBetweenWallets(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Double,
        notes: String = ""
    ) {
        val transferTx = TransactionEntity(
            title = "Transfer Antar Dompet",
            amount = amount,
            type = "TRANSFER",
            categoryId = 1, // default
            walletId = fromWalletId,
            toWalletId = toWalletId,
            dateMillis = System.currentTimeMillis(),
            notes = notes
        )
        insertTransaction(transferTx)
    }

    suspend fun addSavingsContribution(
        goalId: Long,
        fromWalletId: Long,
        amount: Double,
        goalTitle: String
    ) {
        dao.addSavingsContribution(goalId, amount)
        // Record as expense/transfer from wallet
        val tx = TransactionEntity(
            title = "Nabung: $goalTitle",
            amount = amount,
            type = "EXPENSE",
            categoryId = 11, // Investasi / Tabungan
            walletId = fromWalletId,
            dateMillis = System.currentTimeMillis(),
            notes = "Setoran target tabungan $goalTitle"
        )
        insertTransaction(tx)
    }

    suspend fun processRecurringTransaction(recurring: RecurringTransactionEntity) {
        val tx = TransactionEntity(
            title = recurring.title,
            amount = recurring.amount,
            type = recurring.type,
            categoryId = recurring.categoryId,
            walletId = recurring.walletId,
            dateMillis = System.currentTimeMillis(),
            notes = "Auto-entry rutin: ${recurring.notes}"
        )
        insertTransaction(tx)

        // Advance next due date
        val nextCal = Calendar.getInstance().apply { timeInMillis = recurring.nextDueDateMillis }
        when (recurring.frequency) {
            "DAILY" -> nextCal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> nextCal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> nextCal.add(Calendar.MONTH, 1)
        }
        dao.updateRecurring(recurring.copy(nextDueDateMillis = nextCal.timeInMillis))
    }

    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long {
        return dao.insertOrUpdateBudget(budget)
    }

    suspend fun deleteBudget(id: Long) {
        dao.deleteBudgetById(id)
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return dao.insertCategory(category)
    }

    suspend fun insertWallet(wallet: WalletEntity): Long {
        return dao.insertWallet(wallet)
    }

    suspend fun insertRecurring(recurring: RecurringTransactionEntity): Long {
        return dao.insertRecurring(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringTransactionEntity) {
        dao.updateRecurring(recurring)
    }

    suspend fun deleteRecurring(id: Long) {
        dao.deleteRecurringById(id)
    }

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long {
        return dao.insertSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(id: Long) {
        dao.deleteSavingsGoalById(id)
    }

    // --- Notification Operations ---
    suspend fun insertNotification(notification: AppNotificationEntity): Long {
        return dao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Long) {
        dao.markNotificationAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        dao.markAllNotificationsAsRead()
    }

    suspend fun deleteNotification(id: Long) {
        dao.deleteNotificationById(id)
    }

    suspend fun clearAllNotifications() {
        dao.clearAllNotifications()
    }

    // --- Backup & Restore Operations ---
    suspend fun createBackupSnapshot(): BackupData {
        return BackupData(
            categories = dao.getAllCategoriesRaw(),
            wallets = dao.getAllWalletsRaw(),
            transactions = dao.getAllTransactionsRaw(),
            budgets = dao.getAllBudgetsRaw(),
            recurringTransactions = dao.getAllRecurringRaw(),
            savingsGoals = dao.getAllSavingsGoalsRaw(),
            editHistory = dao.getAllEditHistoryRaw(),
            notifications = dao.getAllNotificationsRaw()
        )
    }

    suspend fun restoreFromBackupData(backupData: BackupData) {
        // Clear all current tables
        dao.clearAllTransactions()
        dao.clearAllWallets()
        dao.clearAllCategories()
        dao.clearAllBudgets()
        dao.clearAllRecurring()
        dao.clearAllSavingsGoals()
        dao.clearAllEditHistory()
        dao.clearAllNotifications()

        // Insert new records from backup
        dao.insertCategories(backupData.categories)
        dao.insertWallets(backupData.wallets)
        dao.insertTransactions(backupData.transactions)
        dao.insertBudgets(backupData.budgets)
        dao.insertRecurrings(backupData.recurringTransactions)
        dao.insertSavingsGoals(backupData.savingsGoals)
        dao.insertEditHistories(backupData.editHistory)
        dao.insertNotifications(backupData.notifications)
    }

    suspend fun resetToFreshDatabase() {
        dao.clearAllTransactions()
        dao.clearAllWallets()
        dao.clearAllCategories()
        dao.clearAllBudgets()
        dao.clearAllRecurring()
        dao.clearAllSavingsGoals()
        dao.clearAllEditHistory()
        dao.clearAllNotifications()
        AppDatabase.populateInitialData(dao)
    }

    suspend fun ensureDefaultDataSeeded() {
        if (dao.getCategoryCount() == 0) {
            AppDatabase.populateInitialData(dao)
        }
    }
}
