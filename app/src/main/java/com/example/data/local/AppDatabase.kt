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
            // Default Categories
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
                CategoryEntity(name = "Freelance & Projek", type = "INCOME", iconName = "work", colorHex = "#26C6DA", isDefault = true),
                CategoryEntity(name = "Investasi & Dividen", type = "INCOME", iconName = "trending_up", colorHex = "#8D6E63", isDefault = true),
                CategoryEntity(name = "Bonus & Hadiah", type = "INCOME", iconName = "card_giftcard", colorHex = "#FFCA28", isDefault = true),
                CategoryEntity(name = "Pemasukan Lainnya", type = "INCOME", iconName = "account_balance", colorHex = "#9CCC65", isDefault = true)
            )
            dao.insertCategories(defaultCategories)

            // Default Wallets
            val defaultWallets = listOf(
                WalletEntity(name = "Tunai (Cash)", type = "CASH", balance = 850000.0, iconName = "payments", colorHex = "#43A047"),
                WalletEntity(name = "Bank BCA", type = "BANK", balance = 12450000.0, accountNumber = "123-456-7890", iconName = "account_balance", colorHex = "#00529C"),
                WalletEntity(name = "Bank Mandiri", type = "BANK", balance = 4200000.0, accountNumber = "987-654-3210", iconName = "account_balance", colorHex = "#F58220"),
                WalletEntity(name = "GoPay", type = "E_WALLET", balance = 350000.0, accountNumber = "08123456789", iconName = "phone_android", colorHex = "#00AED6"),
                WalletEntity(name = "ShopeePay", type = "E_WALLET", balance = 180000.0, accountNumber = "08123456789", iconName = "shopping_bag", colorHex = "#EE4D2D")
            )
            dao.insertWallets(defaultWallets)

            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH) + 1
            val currentYear = now.get(Calendar.YEAR)

            // Default Budgets
            val defaultBudgets = listOf(
                BudgetEntity(categoryId = 1, monthlyLimit = 2500000.0, month = currentMonth, year = currentYear), // Makanan
                BudgetEntity(categoryId = 2, monthlyLimit = 800000.0, month = currentMonth, year = currentYear),  // Transport
                BudgetEntity(categoryId = 3, monthlyLimit = 1500000.0, month = currentMonth, year = currentYear), // Belanja
                BudgetEntity(categoryId = 4, monthlyLimit = 1000000.0, month = currentMonth, year = currentYear), // Tagihan
                BudgetEntity(categoryId = 5, monthlyLimit = 600000.0, month = currentMonth, year = currentYear)   // Hiburan
            )
            dao.insertBudgets(defaultBudgets)

            // Default Recurring Transactions
            val calNextWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
            val calNextMonth = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 15) }
            val defaultRecurrings = listOf(
                RecurringTransactionEntity(
                    title = "Langganan Netflix & Spotify",
                    amount = 186000.0,
                    type = "EXPENSE",
                    categoryId = 5,
                    walletId = 2,
                    frequency = "MONTHLY",
                    nextDueDateMillis = calNextWeek.timeInMillis,
                    notes = "Paket Family Sharing"
                ),
                RecurringTransactionEntity(
                    title = "Tagihan Wifi & Listrik PLN",
                    amount = 550000.0,
                    type = "EXPENSE",
                    categoryId = 4,
                    walletId = 2,
                    frequency = "MONTHLY",
                    nextDueDateMillis = calNextMonth.timeInMillis,
                    notes = "Indihome + Token Listrik"
                ),
                RecurringTransactionEntity(
                    title = "Uang Kost / Sewa",
                    amount = 1800000.0,
                    type = "EXPENSE",
                    categoryId = 4,
                    walletId = 2,
                    frequency = "MONTHLY",
                    nextDueDateMillis = calNextMonth.timeInMillis,
                    notes = "Kost bulanan"
                )
            )
            dao.insertRecurrings(defaultRecurrings)

            // Default Savings Goals
            val calVacation = Calendar.getInstance().apply { add(Calendar.MONTH, 4) }
            val calEmergency = Calendar.getInstance().apply { add(Calendar.MONTH, 10) }
            val defaultGoals = listOf(
                SavingsGoalEntity(
                    title = "Liburan ke Bali & Lombok",
                    targetAmount = 6000000.0,
                    currentAmount = 3750000.0,
                    targetDateMillis = calVacation.timeInMillis,
                    iconName = "flight_takeoff",
                    colorHex = "#FF7043",
                    notes = "Target tiket pesawat & hotel"
                ),
                SavingsGoalEntity(
                    title = "Dana Darurat (6 Bulan)",
                    targetAmount = 25000000.0,
                    currentAmount = 16500000.0,
                    targetDateMillis = calEmergency.timeInMillis,
                    iconName = "shield",
                    colorHex = "#26A69A",
                    notes = "Simpan di deposito / reksadana pasar uang"
                ),
                SavingsGoalEntity(
                    title = "Beli Laptop Kerja Baru",
                    targetAmount = 14000000.0,
                    currentAmount = 8200000.0,
                    targetDateMillis = calVacation.timeInMillis,
                    iconName = "laptop",
                    colorHex = "#5C6BC0",
                    notes = "Upgrade spek untuk development"
                )
            )
            dao.insertSavingsGoals(defaultGoals)

            // Sample Recent Transactions (Realistic Indonesian expenses & receipts)
            val today = System.currentTimeMillis()
            val dayMillis = 86400000L
            val sampleTransactions = listOf(
                TransactionEntity(
                    title = "Makan Siang Resto Padang Sederhana",
                    amount = 48000.0,
                    type = "EXPENSE",
                    categoryId = 1,
                    walletId = 1,
                    dateMillis = today - 3600000L * 2,
                    notes = "Ayam pop, sambal hijau, es teh",
                    merchantName = "Restoran Sederhana"
                ),
                TransactionEntity(
                    title = "Bensin Pertamax SPBU Shell",
                    amount = 125000.0,
                    type = "EXPENSE",
                    categoryId = 2,
                    walletId = 4,
                    dateMillis = today - dayMillis * 1,
                    notes = "Isi bensin motor/mobil full tank",
                    merchantName = "Shell Gatot Subroto"
                ),
                TransactionEntity(
                    title = "Belanja Mingguan Supermarket",
                    amount = 385000.0,
                    type = "EXPENSE",
                    categoryId = 3,
                    walletId = 2,
                    dateMillis = today - dayMillis * 2,
                    notes = "Sayur, buah, minyak, perlengkapan mandi",
                    merchantName = "GrandLucky Superstore"
                ),
                TransactionEntity(
                    title = "Kopi & Croissant Cafe",
                    amount = 65000.0,
                    type = "EXPENSE",
                    categoryId = 1,
                    walletId = 4,
                    dateMillis = today - dayMillis * 3,
                    notes = "Ngopi sambil kerja remote",
                    merchantName = "Kopi Kenangan"
                ),
                TransactionEntity(
                    title = "Gaji Pokok Bulan Ini",
                    amount = 11500000.0,
                    type = "INCOME",
                    categoryId = 9,
                    walletId = 2,
                    dateMillis = today - dayMillis * 4,
                    notes = "Transfer payroll kantor",
                    merchantName = "PT Teknologi Maju"
                ),
                TransactionEntity(
                    title = "Pembayaran Projek Mobile App",
                    amount = 4200000.0,
                    type = "INCOME",
                    categoryId = 10,
                    walletId = 3,
                    dateMillis = today - dayMillis * 6,
                    notes = "Termin 1 freelance Kotlin development",
                    merchantName = "Client Startup"
                ),
                TransactionEntity(
                    title = "Nonton Bioskop XXI & Popcorn",
                    amount = 120000.0,
                    type = "EXPENSE",
                    categoryId = 5,
                    walletId = 5,
                    dateMillis = today - dayMillis * 7,
                    notes = "Weekend movie with friends",
                    merchantName = "Cinema XXI Senayan"
                )
            )
            sampleTransactions.forEach { dao.insertTransaction(it) }

            // Initial notifications
            val sampleNotifications = listOf(
                AppNotificationEntity(
                    title = "Selamat Datang di UangKu!",
                    message = "Mulai kelola keuangan, scan struk belanja, dan pantau anggaran Anda dengan mudah.",
                    type = "GENERAL",
                    timestamp = today - 3600000L * 10,
                    isRead = false
                ),
                AppNotificationEntity(
                    title = "Tagihan Segera Jatuh Tempo",
                    message = "Langganan Netflix & Spotify akan jatuh tempo dalam 7 hari.",
                    type = "RECURRING_REMINDER",
                    timestamp = today - 3600000L * 4,
                    isRead = false
                ),
                AppNotificationEntity(
                    title = "Peringatan Anggaran: Makanan & Minuman",
                    message = "Pengeluaran makanan telah mencapai 82% dari batas anggaran bulanan.",
                    type = "BUDGET_ALERT",
                    timestamp = today - 3600000L * 2,
                    isRead = false
                )
            )
            dao.insertNotifications(sampleNotifications)
        }
    }
}
