package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.UserAccount
import com.example.data.local.TransactionWithDetails
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.WalletEntity

@Composable
fun MoreHubScreen(
    wallets: List<WalletEntity>,
    savingsGoals: List<SavingsGoalEntity>,
    recurringList: List<RecurringTransactionEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionWithDetails>,
    deletedTransactions: List<TransactionWithDetails>,
    totalIncome: Double,
    totalExpense: Double,
    isDarkMode: Boolean?,
    onToggleDarkMode: (Boolean) -> Unit,
    onAddWallet: (String, String, Double, String, String) -> Unit,
    onTransfer: (Long, Long, Double, String) -> Unit,
    onAddGoal: (String, Double, Long, String, String, String) -> Unit,
    onContributeGoal: (Long, Long, Double, String, Boolean) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddRecurring: (String, Double, String, Long, Long, String, String) -> Unit,
    onProcessRecurringNow: (RecurringTransactionEntity) -> Unit,
    onToggleRecurringActive: (RecurringTransactionEntity) -> Unit,
    onDeleteRecurring: (Long) -> Unit,
    onTriggerRecurringNotification: (RecurringTransactionEntity) -> Unit = {},
    onExportCsv: suspend (Context) -> Uri?,
    // Trash
    onRestoreDeletedTransaction: (Long) -> Unit,
    onPermanentDeleteTransaction: (Long) -> Unit,
    onClearTrash: () -> Unit,
    // Backup
    isAutoSync: Boolean,
    onToggleAutoSync: (Boolean) -> Unit,
    lastBackupTime: Long?,
    isBackupInProgress: Boolean,
    backupStatusMessage: String?,
    userAccount: UserAccount? = null,
    isFirebaseActive: Boolean = false,
    onOpenGoogleAccount: () -> Unit = {},
    onTriggerManualBackup: () -> Unit,
    onRestoreFromCloud: ((Boolean, String) -> Unit) -> Unit,
    onExportBackupJson: suspend (Context) -> Uri?,
    onRestoreFromJson: (String, (Boolean, String) -> Unit) -> Unit,
    onResetAllData: ((Boolean, String) -> Unit) -> Unit = {},
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(initialTab) }

    val tabs = listOf(
        "Dompet" to Icons.Default.AccountBalanceWallet,
        "Tabungan" to Icons.Default.Savings,
        "Rutin" to Icons.Default.Repeat,
        "Laporan" to Icons.Default.FileDownload,
        "Sampah" to Icons.Default.Delete,
        "Cadangan" to Icons.Default.CloudSync
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                val isTrash = index == 4
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    icon = {
                        if (isTrash && deletedTransactions.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text("${deletedTransactions.size}") } }) {
                                Icon(imageVector = icon, contentDescription = label)
                            }
                        } else {
                            Icon(imageVector = icon, contentDescription = label)
                        }
                    },
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedSubTab) {
            0 -> WalletsScreen(
                wallets = wallets,
                onAddWallet = onAddWallet,
                onTransfer = onTransfer
            )
            1 -> SavingsGoalsScreen(
                goals = savingsGoals,
                wallets = wallets,
                onAddGoal = onAddGoal,
                onContribute = { id, wId, amt, title, isSynced -> onContributeGoal(id, wId, amt, title, isSynced) },
                onDeleteGoal = onDeleteGoal
            )
            2 -> RecurringScreen(
                recurringList = recurringList,
                categories = categories,
                wallets = wallets,
                onAddRecurring = onAddRecurring,
                onProcessNow = onProcessRecurringNow,
                onToggleActive = onToggleRecurringActive,
                onDelete = onDeleteRecurring,
                onTriggerReminderNotification = onTriggerRecurringNotification
            )
            3 -> ExportReportScreen(
                transactions = transactions,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onExportCsv = onExportCsv
            )
            4 -> TrashScreen(
                deletedTransactions = deletedTransactions,
                onRestoreTransaction = onRestoreDeletedTransaction,
                onPermanentDelete = onPermanentDeleteTransaction,
                onClearTrash = onClearTrash
            )
            5 -> BackupScreen(
                isAutoSync = isAutoSync,
                onToggleAutoSync = onToggleAutoSync,
                lastBackupTime = lastBackupTime,
                isBackupInProgress = isBackupInProgress,
                backupStatusMessage = backupStatusMessage,
                userAccount = userAccount,
                isFirebaseActive = isFirebaseActive,
                onOpenGoogleAccount = onOpenGoogleAccount,
                onTriggerManualBackup = onTriggerManualBackup,
                onRestoreFromCloud = onRestoreFromCloud,
                onExportBackupJson = onExportBackupJson,
                onRestoreFromJson = onRestoreFromJson,
                onResetAllData = onResetAllData
            )
        }
    }
}
