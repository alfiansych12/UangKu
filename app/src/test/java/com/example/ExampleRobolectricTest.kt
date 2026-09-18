package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupData
import com.example.data.backup.BackupManager
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WalletEntity
import com.example.ui.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("UangKu", appName)
    }

    @Test
    fun `format rupiah correctly formats positive numbers`() {
        val formatted = Formatters.formatRupiah(75000.0)
        assertTrue(formatted.contains("75.000") || formatted.contains("75,000"))
    }

    @Test
    fun `format percentage correctly formats ratio`() {
        val formatted = Formatters.formatPercentage(0.825)
        assertTrue(formatted.contains("82") || formatted.contains("83"))
    }

    @Test
    fun `backup manager serialization round trip integrity`() {
        val backup = BackupData(
            version = 2,
            backupDateMillis = 1700000000000L,
            categories = listOf(
                CategoryEntity(id = 1, name = "Makanan", type = "EXPENSE", iconName = "restaurant", colorHex = "#FF5722")
            ),
            wallets = listOf(
                WalletEntity(id = 1, name = "Dompet Utama", type = "CASH", balance = 250000.0, accountNumber = "", colorHex = "#4CAF50")
            ),
            transactions = listOf(
                TransactionEntity(
                    id = 10,
                    title = "Nasi Padang",
                    amount = 25000.0,
                    type = "EXPENSE",
                    categoryId = 1,
                    walletId = 1,
                    dateMillis = 1700000000000L,
                    notes = "Makan siang"
                )
            )
        )

        val json = BackupManager.toJson(backup)
        assertNotNull(json)
        assertTrue(json.contains("Nasi Padang"))

        val restored = BackupManager.fromJson(json)
        assertEquals(1, restored.transactions.size)
        assertEquals("Nasi Padang", restored.transactions[0].title)
        assertEquals(25000.0, restored.transactions[0].amount, 0.01)
    }

    @Test
    fun `google auth manager handles local account state and sign out`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authManager = com.example.data.auth.GoogleAuthManager(context)

        // Quick sign in for testing
        authManager.signInQuick("test@gmail.com", "Test User")
        val saved = authManager.getSavedUser()
        assertNotNull(saved)
        assertEquals("test@gmail.com", saved?.email)
        assertEquals("Test User", saved?.displayName)

        // Sign out
        authManager.signOut(null)
        val afterSignOut = authManager.getSavedUser()
        assertEquals(null, afterSignOut)
    }
}
