package com.example.data.backup

import android.content.Context
import android.os.Build
import com.example.data.model.AppNotificationEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEditHistoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WalletEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val version: Int = 1,
    val appName: String = "UangKu",
    val backupDateMillis: Long = System.currentTimeMillis(),
    val formattedDate: String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date()),
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val categories: List<CategoryEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val recurringTransactions: List<RecurringTransactionEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val editHistory: List<TransactionEditHistoryEntity> = emptyList(),
    val notifications: List<AppNotificationEntity> = emptyList()
)

object BackupManager {
    private const val PREFS_NAME = "uangku_backup_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_timestamp"
    private const val KEY_AUTO_SYNC = "auto_sync_enabled"
    private const val CLOUD_BACKUP_FILENAME = "uangku_cloud_snapshot.json"

    fun getLastBackupTime(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = prefs.getLong(KEY_LAST_BACKUP_TIME, -1L)
        return if (time > 0) time else null
    }

    fun setLastBackupTime(context: Context, timeMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, timeMillis).apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun toJson(data: BackupData): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("appName", data.appName)
        root.put("backupDateMillis", data.backupDateMillis)
        root.put("formattedDate", data.formattedDate)
        root.put("deviceModel", data.deviceModel)

        // Categories
        val catArray = JSONArray()
        data.categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("type", c.type)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isDefault", c.isDefault)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Wallets
        val walletArray = JSONArray()
        data.wallets.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("name", w.name)
            obj.put("type", w.type)
            obj.put("balance", w.balance)
            obj.put("accountNumber", w.accountNumber)
            obj.put("iconName", w.iconName)
            obj.put("colorHex", w.colorHex)
            walletArray.put(obj)
        }
        root.put("wallets", walletArray)

        // Transactions
        val txArray = JSONArray()
        data.transactions.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("amount", t.amount)
            obj.put("type", t.type)
            obj.put("categoryId", t.categoryId)
            obj.put("walletId", t.walletId)
            if (t.toWalletId != null) obj.put("toWalletId", t.toWalletId)
            obj.put("dateMillis", t.dateMillis)
            obj.put("notes", t.notes)
            if (t.receiptImagePath != null) obj.put("receiptImagePath", t.receiptImagePath)
            obj.put("tags", t.tags)
            obj.put("merchantName", t.merchantName)
            obj.put("isEdited", t.isEdited)
            obj.put("modifiedAt", t.modifiedAt)
            obj.put("isDeleted", t.isDeleted)
            if (t.deletedAt != null) obj.put("deletedAt", t.deletedAt)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Budgets
        val budgetArray = JSONArray()
        data.budgets.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("categoryId", b.categoryId)
            obj.put("monthlyLimit", b.monthlyLimit)
            obj.put("month", b.month)
            obj.put("year", b.year)
            budgetArray.put(obj)
        }
        root.put("budgets", budgetArray)

        // Recurring
        val recArray = JSONArray()
        data.recurringTransactions.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("title", r.title)
            obj.put("amount", r.amount)
            obj.put("type", r.type)
            obj.put("categoryId", r.categoryId)
            obj.put("walletId", r.walletId)
            obj.put("frequency", r.frequency)
            obj.put("nextDueDateMillis", r.nextDueDateMillis)
            obj.put("isActive", r.isActive)
            obj.put("notes", r.notes)
            recArray.put(obj)
        }
        root.put("recurringTransactions", recArray)

        // Savings Goals
        val goalsArray = JSONArray()
        data.savingsGoals.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("title", g.title)
            obj.put("targetAmount", g.targetAmount)
            obj.put("currentAmount", g.currentAmount)
            obj.put("targetDateMillis", g.targetDateMillis)
            obj.put("iconName", g.iconName)
            obj.put("colorHex", g.colorHex)
            obj.put("notes", g.notes)
            goalsArray.put(obj)
        }
        root.put("savingsGoals", goalsArray)

        // Edit History
        val historyArray = JSONArray()
        data.editHistory.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("transactionId", h.transactionId)
            obj.put("editTimestamp", h.editTimestamp)
            obj.put("previousTitle", h.previousTitle)
            obj.put("previousAmount", h.previousAmount)
            obj.put("previousType", h.previousType)
            obj.put("previousCategoryId", h.previousCategoryId)
            obj.put("previousWalletId", h.previousWalletId)
            if (h.previousToWalletId != null) obj.put("previousToWalletId", h.previousToWalletId)
            obj.put("previousDateMillis", h.previousDateMillis)
            obj.put("previousNotes", h.previousNotes)
            obj.put("previousMerchantName", h.previousMerchantName)
            obj.put("newTitle", h.newTitle)
            obj.put("newAmount", h.newAmount)
            obj.put("newType", h.newType)
            obj.put("newCategoryId", h.newCategoryId)
            obj.put("newWalletId", h.newWalletId)
            if (h.newToWalletId != null) obj.put("newToWalletId", h.newToWalletId)
            obj.put("newDateMillis", h.newDateMillis)
            obj.put("newNotes", h.newNotes)
            obj.put("newMerchantName", h.newMerchantName)
            obj.put("reason", h.reason)
            historyArray.put(obj)
        }
        root.put("editHistory", historyArray)

        // Notifications
        val notifArray = JSONArray()
        data.notifications.forEach { n ->
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("message", n.message)
            obj.put("type", n.type)
            obj.put("timestamp", n.timestamp)
            obj.put("isRead", n.isRead)
            obj.put("actionPayload", n.actionPayload)
            notifArray.put(obj)
        }
        root.put("notifications", notifArray)

        return root.toString(2)
    }

    fun fromJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val appName = root.optString("appName", "UangKu")
        val backupDate = root.optLong("backupDateMillis", System.currentTimeMillis())
        val formattedDate = root.optString("formattedDate", "")
        val deviceModel = root.optString("deviceModel", "Unknown")

        // Categories
        val catList = mutableListOf<CategoryEntity>()
        val catArray = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArray.length()) {
            val obj = catArray.getJSONObject(i)
            catList.add(
                CategoryEntity(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name"),
                    type = obj.optString("type", "EXPENSE"),
                    iconName = obj.optString("iconName", "category"),
                    colorHex = obj.optString("colorHex", "#78909C"),
                    isDefault = obj.optBoolean("isDefault", false)
                )
            )
        }

        // Wallets
        val walletList = mutableListOf<WalletEntity>()
        val walletArray = root.optJSONArray("wallets") ?: JSONArray()
        for (i in 0 until walletArray.length()) {
            val obj = walletArray.getJSONObject(i)
            walletList.add(
                WalletEntity(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name"),
                    type = obj.optString("type", "CASH"),
                    balance = obj.optDouble("balance", 0.0),
                    accountNumber = obj.optString("accountNumber", ""),
                    iconName = obj.optString("iconName", "account_balance_wallet"),
                    colorHex = obj.optString("colorHex", "#1E88E5")
                )
            )
        }

        // Transactions
        val txList = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            txList.add(
                TransactionEntity(
                    id = obj.optLong("id", 0),
                    title = obj.optString("title"),
                    amount = obj.optDouble("amount", 0.0),
                    type = obj.optString("type", "EXPENSE"),
                    categoryId = obj.optLong("categoryId", 1),
                    walletId = obj.optLong("walletId", 1),
                    toWalletId = if (obj.has("toWalletId")) obj.optLong("toWalletId") else null,
                    dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                    notes = obj.optString("notes", ""),
                    receiptImagePath = if (obj.has("receiptImagePath")) obj.optString("receiptImagePath") else null,
                    tags = obj.optString("tags", ""),
                    merchantName = obj.optString("merchantName", ""),
                    isEdited = obj.optBoolean("isEdited", false),
                    modifiedAt = obj.optLong("modifiedAt", 0L),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    deletedAt = if (obj.has("deletedAt")) obj.optLong("deletedAt") else null
                )
            )
        }

        // Budgets
        val budgetList = mutableListOf<BudgetEntity>()
        val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgetArray.length()) {
            val obj = budgetArray.getJSONObject(i)
            budgetList.add(
                BudgetEntity(
                    id = obj.optLong("id", 0),
                    categoryId = obj.optLong("categoryId", 1),
                    monthlyLimit = obj.optDouble("monthlyLimit", 0.0),
                    month = obj.optInt("month", 1),
                    year = obj.optInt("year", 2025)
                )
            )
        }

        // Recurring
        val recList = mutableListOf<RecurringTransactionEntity>()
        val recArray = root.optJSONArray("recurringTransactions") ?: JSONArray()
        for (i in 0 until recArray.length()) {
            val obj = recArray.getJSONObject(i)
            recList.add(
                RecurringTransactionEntity(
                    id = obj.optLong("id", 0),
                    title = obj.optString("title"),
                    amount = obj.optDouble("amount", 0.0),
                    type = obj.optString("type", "EXPENSE"),
                    categoryId = obj.optLong("categoryId", 1),
                    walletId = obj.optLong("walletId", 1),
                    frequency = obj.optString("frequency", "MONTHLY"),
                    nextDueDateMillis = obj.optLong("nextDueDateMillis", System.currentTimeMillis()),
                    isActive = obj.optBoolean("isActive", true),
                    notes = obj.optString("notes", "")
                )
            )
        }

        // Goals
        val goalsList = mutableListOf<SavingsGoalEntity>()
        val goalsArray = root.optJSONArray("savingsGoals") ?: JSONArray()
        for (i in 0 until goalsArray.length()) {
            val obj = goalsArray.getJSONObject(i)
            goalsList.add(
                SavingsGoalEntity(
                    id = obj.optLong("id", 0),
                    title = obj.optString("title"),
                    targetAmount = obj.optDouble("targetAmount", 0.0),
                    currentAmount = obj.optDouble("currentAmount", 0.0),
                    targetDateMillis = obj.optLong("targetDateMillis", System.currentTimeMillis()),
                    iconName = obj.optString("iconName", "savings"),
                    colorHex = obj.optString("colorHex", "#00897B"),
                    notes = obj.optString("notes", "")
                )
            )
        }

        // Edit History
        val historyList = mutableListOf<TransactionEditHistoryEntity>()
        val historyArray = root.optJSONArray("editHistory") ?: JSONArray()
        for (i in 0 until historyArray.length()) {
            val obj = historyArray.getJSONObject(i)
            historyList.add(
                TransactionEditHistoryEntity(
                    id = obj.optLong("id", 0),
                    transactionId = obj.optLong("transactionId"),
                    editTimestamp = obj.optLong("editTimestamp", System.currentTimeMillis()),
                    previousTitle = obj.optString("previousTitle"),
                    previousAmount = obj.optDouble("previousAmount"),
                    previousType = obj.optString("previousType"),
                    previousCategoryId = obj.optLong("previousCategoryId"),
                    previousWalletId = obj.optLong("previousWalletId"),
                    previousToWalletId = if (obj.has("previousToWalletId")) obj.optLong("previousToWalletId") else null,
                    previousDateMillis = obj.optLong("previousDateMillis"),
                    previousNotes = obj.optString("previousNotes", ""),
                    previousMerchantName = obj.optString("previousMerchantName", ""),
                    newTitle = obj.optString("newTitle"),
                    newAmount = obj.optDouble("newAmount"),
                    newType = obj.optString("newType"),
                    newCategoryId = obj.optLong("newCategoryId"),
                    newWalletId = obj.optLong("newWalletId"),
                    newToWalletId = if (obj.has("newToWalletId")) obj.optLong("newToWalletId") else null,
                    newDateMillis = obj.optLong("newDateMillis"),
                    newNotes = obj.optString("newNotes", ""),
                    newMerchantName = obj.optString("newMerchantName", ""),
                    reason = obj.optString("reason", "")
                )
            )
        }

        // Notifications
        val notifList = mutableListOf<AppNotificationEntity>()
        val notifArray = root.optJSONArray("notifications") ?: JSONArray()
        for (i in 0 until notifArray.length()) {
            val obj = notifArray.getJSONObject(i)
            notifList.add(
                AppNotificationEntity(
                    id = obj.optLong("id", 0),
                    title = obj.optString("title"),
                    message = obj.optString("message"),
                    type = obj.optString("type", "GENERAL"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    isRead = obj.optBoolean("isRead", false),
                    actionPayload = obj.optString("actionPayload", "")
                )
            )
        }

        return BackupData(
            version = version,
            appName = appName,
            backupDateMillis = backupDate,
            formattedDate = formattedDate,
            deviceModel = deviceModel,
            categories = catList,
            wallets = walletList,
            transactions = txList,
            budgets = budgetList,
            recurringTransactions = recList,
            savingsGoals = goalsList,
            editHistory = historyList,
            notifications = notifList
        )
    }

    fun saveCloudSnapshot(context: Context, jsonString: String) {
        val file = File(context.filesDir, CLOUD_BACKUP_FILENAME)
        file.writeText(jsonString)
        setLastBackupTime(context, System.currentTimeMillis())
    }

    fun getCloudSnapshot(context: Context): String? {
        val file = File(context.filesDir, CLOUD_BACKUP_FILENAME)
        return if (file.exists()) file.readText() else null
    }

    fun saveBackupToFile(context: Context, jsonString: String): File {
        val dir = File(context.cacheDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "UangKu_Backup_$timestamp.json")
        file.writeText(jsonString)
        return file
    }
}
