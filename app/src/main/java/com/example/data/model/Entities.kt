package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class WalletType {
    CASH,
    BANK,
    E_WALLET
}

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // EXPENSE or INCOME
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // CASH, BANK, E_WALLET
    val balance: Double,
    val accountNumber: String = "",
    val iconName: String = "account_balance_wallet",
    val colorHex: String = "#1E88E5"
)

@Entity(
    tableName = "transactions",
    indices = [Index("categoryId"), Index("walletId"), Index("dateMillis"), Index("isDeleted")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val categoryId: Long,
    val walletId: Long,
    val toWalletId: Long? = null, // for TRANSFER
    val dateMillis: Long,
    val notes: String = "",
    val receiptImagePath: String? = null,
    val tags: String = "",
    val merchantName: String = "",
    val isEdited: Boolean = false,
    val modifiedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "transaction_edit_history",
    indices = [Index("transactionId")]
)
data class TransactionEditHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val editTimestamp: Long = System.currentTimeMillis(),
    val previousTitle: String,
    val previousAmount: Double,
    val previousType: String,
    val previousCategoryId: Long,
    val previousWalletId: Long,
    val previousToWalletId: Long? = null,
    val previousDateMillis: Long,
    val previousNotes: String = "",
    val previousMerchantName: String = "",
    val newTitle: String,
    val newAmount: Double,
    val newType: String,
    val newCategoryId: Long,
    val newWalletId: Long,
    val newToWalletId: Long? = null,
    val newDateMillis: Long,
    val newNotes: String = "",
    val newMerchantName: String = "",
    val reason: String = "Perubahan data transaksi"
)

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // BUDGET_ALERT, RECURRING_REMINDER, BACKUP_SUCCESS, BACKUP_ERROR, GENERAL
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionPayload: String = ""
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "month", "year"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Double,
    val month: Int, // 1 - 12
    val year: Int
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // EXPENSE, INCOME
    val categoryId: Long,
    val walletId: Long,
    val frequency: String, // DAILY, WEEKLY, MONTHLY
    val nextDueDateMillis: Long,
    val isActive: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDateMillis: Long,
    val iconName: String = "savings",
    val colorHex: String = "#00897B",
    val notes: String = ""
)
