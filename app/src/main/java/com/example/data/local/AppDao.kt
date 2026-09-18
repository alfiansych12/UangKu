package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AppNotificationEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEditHistoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WalletEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithDetails(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val walletId: Long,
    val walletName: String?,
    val toWalletId: Long?,
    val toWalletName: String?,
    val dateMillis: Long,
    val notes: String,
    val receiptImagePath: String?,
    val tags: String,
    val merchantName: String,
    val isEdited: Boolean = false,
    val modifiedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Dao
interface AppDao {
    // --- Transactions ---
    @Query("""
        SELECT t.id, t.title, t.amount, t.type, t.categoryId,
               c.name AS categoryName, c.iconName AS categoryIcon, c.colorHex AS categoryColor,
               t.walletId, w.name AS walletName,
               t.toWalletId, tw.name AS toWalletName,
               t.dateMillis, t.notes, t.receiptImagePath, t.tags, t.merchantName,
               t.isEdited, t.modifiedAt, t.isDeleted, t.deletedAt
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN wallets w ON t.walletId = w.id
        LEFT JOIN wallets tw ON t.toWalletId = tw.id
        WHERE t.isDeleted = 0
        ORDER BY t.dateMillis DESC, t.id DESC
    """)
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT t.id, t.title, t.amount, t.type, t.categoryId,
               c.name AS categoryName, c.iconName AS categoryIcon, c.colorHex AS categoryColor,
               t.walletId, w.name AS walletName,
               t.toWalletId, tw.name AS toWalletName,
               t.dateMillis, t.notes, t.receiptImagePath, t.tags, t.merchantName,
               t.isEdited, t.modifiedAt, t.isDeleted, t.deletedAt
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN wallets w ON t.walletId = w.id
        LEFT JOIN wallets tw ON t.toWalletId = tw.id
        WHERE t.isDeleted = 1
        ORDER BY t.deletedAt DESC, t.id DESC
    """)
    fun getDeletedTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT t.id, t.title, t.amount, t.type, t.categoryId,
               c.name AS categoryName, c.iconName AS categoryIcon, c.colorHex AS categoryColor,
               t.walletId, w.name AS walletName,
               t.toWalletId, tw.name AS toWalletName,
               t.dateMillis, t.notes, t.receiptImagePath, t.tags, t.merchantName,
               t.isEdited, t.modifiedAt, t.isDeleted, t.deletedAt
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN wallets w ON t.walletId = w.id
        LEFT JOIN wallets tw ON t.toWalletId = tw.id
        WHERE t.isDeleted = 0 AND t.dateMillis >= :startDateMillis AND t.dateMillis <= :endDateMillis
        ORDER BY t.dateMillis DESC, t.id DESC
    """)
    fun getTransactionsBetween(startDateMillis: Long, endDateMillis: Long): Flow<List<TransactionWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, deletedAt: Long)

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTransaction(id: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE isDeleted = 1")
    suspend fun clearTrash(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 1")
    fun getDeletedTransactionsCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsRaw(): List<TransactionEntity>

    // --- Edit History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditHistory(history: TransactionEditHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditHistories(histories: List<TransactionEditHistoryEntity>)

    @Query("SELECT * FROM transaction_edit_history WHERE transactionId = :txId ORDER BY editTimestamp DESC")
    fun getEditHistoryForTransaction(txId: Long): Flow<List<TransactionEditHistoryEntity>>

    @Query("SELECT * FROM transaction_edit_history ORDER BY editTimestamp DESC")
    fun getAllEditHistory(): Flow<List<TransactionEditHistoryEntity>>

    @Query("SELECT * FROM transaction_edit_history")
    suspend fun getAllEditHistoryRaw(): List<TransactionEditHistoryEntity>

    @Query("DELETE FROM transaction_edit_history WHERE transactionId = :txId")
    suspend fun deleteEditHistoryForTransaction(txId: Long)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY type ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // --- Wallets ---
    @Query("SELECT * FROM wallets ORDER BY id ASC")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWallets(wallets: List<WalletEntity>)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET balance = balance + :delta WHERE id = :walletId")
    suspend fun updateWalletBalance(walletId: Long, delta: Double)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteWalletById(id: Long)

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    // --- Recurring Transactions ---
    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDateMillis ASC")
    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecurrings(recurrings: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringById(id: Long)

    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY targetDateMillis ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :amount WHERE id = :id")
    suspend fun addSavingsContribution(id: Long, amount: Double)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // --- Notifications ---
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0")
    fun getUnreadNotificationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotificationEntity>)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM app_notifications")
    suspend fun clearAllNotifications()

    @Query("SELECT * FROM app_notifications")
    suspend fun getAllNotificationsRaw(): List<AppNotificationEntity>

    // --- Raw queries & clear tables for Backup & Restore ---
    @Query("SELECT * FROM wallets")
    suspend fun getAllWalletsRaw(): List<WalletEntity>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesRaw(): List<CategoryEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsRaw(): List<BudgetEntity>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRecurringRaw(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllSavingsGoalsRaw(): List<SavingsGoalEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM wallets")
    suspend fun clearAllWallets()

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAllRecurring()

    @Query("DELETE FROM savings_goals")
    suspend fun clearAllSavingsGoals()

    @Query("DELETE FROM transaction_edit_history")
    suspend fun clearAllEditHistory()
}
