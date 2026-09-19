package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppNotificationEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEditHistoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WalletEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        CategoryEntity::class,
        WalletEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        SavingsGoalEntity::class,
        TransactionEditHistoryEntity::class,
        AppNotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uangku_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.appDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: AppDao) {
            // 1. Default Essential Categories
            val defaultCategories = listOf(
                // Pengeluaran
                CategoryEntity(name = "Makanan & Minuman", type = "EXPENSE", iconName = "restaurant", colorHex = "#FF7043", isDefault = true),
                CategoryEntity(name = "Transportasi", type = "EXPENSE", iconName = "directions_car", colorHex = "#42A5F5", isDefault = true),
                CategoryEntity(name = "Belanja & Kebutuhan", type = "EXPENSE", iconName = "shopping_cart", colorHex = "#AB47BC", isDefault = true),
                CategoryEntity(name = "Tagihan & Utilitas", type = "EXPENSE", iconName = "bolt", colorHex = "#FFA726", isDefault = true),
                CategoryEntity(name = "Hiburan & Rekreasi", type = "EXPENSE", iconName = "movie", colorHex = "#EC407A", isDefault = true),
                CategoryEntity(name = "Kesehatan & Medis", type = "EXPENSE", iconName = "medical_services", colorHex = "#26A69A", isDefault = true),
                CategoryEntity(name = "Pendidikan", type = "EXPENSE", iconName = "school", colorHex = "#5C6BC0", isDefault = true),
                CategoryEntity(name = "Lain-lain", type = "EXPENSE", iconName = "category", colorHex = "#78909C", isDefault = true),

                // Pemasukan
                CategoryEntity(name = "Gaji Bulanan", type = "INCOME", iconName = "payments", colorHex = "#66BB6A", isDefault = true),
                CategoryEntity(name = "Freelance & Bisnis", type = "INCOME", iconName = "work", colorHex = "#26C6DA", isDefault = true),
                CategoryEntity(name = "Investasi & Dividen", type = "INCOME", iconName = "trending_up", colorHex = "#8D6E63", isDefault = true),
                CategoryEntity(name = "Bonus & Hadiah", type = "INCOME", iconName = "card_giftcard", colorHex = "#FFCA28", isDefault = true),
                CategoryEntity(name = "Pemasukan Lainnya", type = "INCOME", iconName = "account_balance", colorHex = "#9CCC65", isDefault = true)
            )
            dao.insertCategories(defaultCategories)

            // 2. Default Initial Wallets (Clean 0 Balance)
            val defaultWallets = listOf(
                WalletEntity(name = "Tunai (Cash)", type = "CASH", balance = 0.0, iconName = "payments", colorHex = "#43A047"),
                WalletEntity(name = "Rekening Bank", type = "BANK", balance = 0.0, accountNumber = "", iconName = "account_balance", colorHex = "#00529C")
            )
            dao.insertWallets(defaultWallets)

            // Transactions, Budgets, Savings Goals, Recurrings are strictly EMPTY by default.
            // Welcome notification
            val welcomeNotification = AppNotificationEntity(
                title = "Selamat Datang di UangKu!",
                message = "Aplikasi siap digunakan. Mulai catat pemasukan, pengeluaran, atau scan struk belanja Anda.",
                type = "GENERAL",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            dao.insertNotification(welcomeNotification)
        }
    }
}
