package com.example.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.auth.AuthState
import com.example.data.local.TransactionWithDetails
import com.example.network.ParsedReceipt
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.GoogleAccountDialog
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.ScanReceiptSheet
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.UangKuViewModel

enum class NavigationTab(val label: String) {
    HOME("Beranda"),
    TRANSACTIONS("Transaksi"),
    ANALYTICS("Statistik"),
    BUDGETS("Anggaran"),
    MORE("Lainnya")
}

@Composable
fun UangKuApp(viewModel: UangKuViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initBackupPrefs(context)
    }

    // State collections
    val transactions by viewModel.transactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val deletedTransactions by viewModel.deletedTransactions.collectAsState()
    val deletedCount by viewModel.deletedTransactionsCount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val recurring by viewModel.recurring.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()

    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalBalance by viewModel.totalWalletsBalance.collectAsState()

    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val monthlyTrends by viewModel.monthlyTrends.collectAsState()
    val budgetAlerts by viewModel.budgetAlerts.collectAsState()
    val insights by viewModel.insights.collectAsState()

    val notifications by viewModel.notifications.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedWalletId by viewModel.selectedWalletId.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val minAmountFilter by viewModel.minAmountFilter.collectAsState()
    val maxAmountFilter by viewModel.maxAmountFilter.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val activeFilterCount by viewModel.activeFilterCount.collectAsState()

    val isAutoSync by viewModel.isAutoSync.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val isBackupInProgress by viewModel.isBackupInProgress.collectAsState()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scannedReceipt by viewModel.scannedReceipt.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val effectiveDarkTheme = isDarkMode ?: systemDark

    val authState by viewModel.authState.collectAsState()
    val currentAccount = (authState as? AuthState.Authenticated)?.user

    // Navigation & Sheet States
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var moreSubTab by remember { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var showScanSheet by remember { mutableStateOf(false) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    var editingTransaction by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var pendingReceiptForAdd by remember { mutableStateOf<ParsedReceipt?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionWithDetails?>(null) }

    MyApplicationTheme(darkTheme = effectiveDarkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.HOME,
                        onClick = { currentTab = NavigationTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda", fontWeight = if (currentTab == NavigationTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_home")
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.TRANSACTIONS,
                        onClick = { currentTab = NavigationTab.TRANSACTIONS },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Transaksi") },
                        label = { Text("Transaksi", fontWeight = if (currentTab == NavigationTab.TRANSACTIONS) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_transactions")
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.ANALYTICS,
                        onClick = { currentTab = NavigationTab.ANALYTICS },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = "Statistik") },
                        label = { Text("Statistik", fontWeight = if (currentTab == NavigationTab.ANALYTICS) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.BUDGETS,
                        onClick = { currentTab = NavigationTab.BUDGETS },
                        icon = { Icon(Icons.Default.PriceCheck, contentDescription = "Anggaran") },
                        label = { Text("Anggaran", fontWeight = if (currentTab == NavigationTab.BUDGETS) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_budgets")
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.MORE,
                        onClick = { currentTab = NavigationTab.MORE },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Lainnya") },
                        label = { Text("Lainnya", fontWeight = if (currentTab == NavigationTab.MORE) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_more")
                    )
                }
            },
            floatingActionButton = {
                // Show Add transaction FAB on Home and Transactions screen
                if (currentTab == NavigationTab.HOME || currentTab == NavigationTab.TRANSACTIONS) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            pendingReceiptForAdd = null
                            showAddTransactionSheet = true
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Transaksi")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            totalBalance = totalBalance,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            wallets = wallets,
                            recentTransactions = transactions,
                            budgetAlerts = budgetAlerts,
                            savingsGoals = savingsGoals,
                            userAccount = currentAccount,
                            onOpenGoogleAccount = { showGoogleAccountDialog = true },
                            unreadNotificationCount = unreadNotificationCount,
                            onOpenNotifications = { showNotificationsSheet = true },
                            onOpenOcrScanner = {
                                viewModel.clearScannedReceipt()
                                showScanSheet = true
                            },
                            onOpenAddTransaction = {
                                editingTransaction = null
                                pendingReceiptForAdd = null
                                showAddTransactionSheet = true
                            },
                            onOpenTransfer = {
                                moreSubTab = 0
                                currentTab = NavigationTab.MORE
                            },
                            onTransactionClick = { tx ->
                                selectedTransactionForDetail = tx
                            },
                            onNavigateToTransactions = {
                                currentTab = NavigationTab.TRANSACTIONS
                            },
                            onNavigateToWallets = {
                                moreSubTab = 0
                                currentTab = NavigationTab.MORE
                            },
                            onNavigateToBudgets = {
                                currentTab = NavigationTab.BUDGETS
                            },
                            onNavigateToSavings = {
                                moreSubTab = 1
                                currentTab = NavigationTab.MORE
                            },
                            onNavigateToTrash = {
                                moreSubTab = 4
                                currentTab = NavigationTab.MORE
                            }
                        )
                    }

                    NavigationTab.TRANSACTIONS -> {
                        TransactionsScreen(
                            transactions = filteredTransactions,
                            categories = categories,
                            wallets = wallets,
                            searchQuery = searchQuery,
                            onSearchChange = {
                                viewModel.searchQuery.value = it
                                if (it.isNotBlank()) viewModel.addSearchToHistory(it)
                            },
                            searchHistory = searchHistory,
                            selectedTypeFilter = selectedTypeFilter,
                            onTypeFilterChange = { viewModel.selectedTypeFilter.value = it },
                            selectedCategoryId = selectedCategoryId,
                            onCategoryFilterChange = { viewModel.selectedCategoryId.value = it },
                            selectedWalletId = selectedWalletId,
                            onWalletFilterChange = { viewModel.selectedWalletId.value = it },
                            selectedTimeRange = selectedTimeRange,
                            onTimeRangeChange = { viewModel.selectedTimeRange.value = it },
                            minAmount = minAmountFilter,
                            maxAmount = maxAmountFilter,
                            onMinAmountChange = { viewModel.minAmountFilter.value = it },
                            onMaxAmountChange = { viewModel.maxAmountFilter.value = it },
                            sortBy = sortBy,
                            onSortChange = { viewModel.sortBy.value = it },
                            activeFilterCount = activeFilterCount,
                            onResetFilters = { viewModel.resetFilters() },
                            onTransactionClick = { tx ->
                                selectedTransactionForDetail = tx
                            },
                            onTransactionLongClick = { tx ->
                                transactionToDelete = tx
                            }
                        )
                    }

                    NavigationTab.ANALYTICS -> {
                        AnalyticsScreen(
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            categoryBreakdown = categoryBreakdown,
                            monthlyTrends = monthlyTrends,
                            insights = insights
                        )
                    }

                    NavigationTab.BUDGETS -> {
                        BudgetsScreen(
                            budgetAlerts = budgetAlerts,
                            categories = categories,
                            onSaveBudget = { catId, limit ->
                                viewModel.setBudget(catId, limit)
                            },
                            onDeleteBudget = { catId ->
                                val b = budgets.find { it.categoryId == catId }
                                if (b != null) viewModel.deleteBudget(b.id)
                            }
                        )
                    }

                    NavigationTab.MORE -> {
                        MoreHubScreen(
                            wallets = wallets,
                            savingsGoals = savingsGoals,
                            recurringList = recurring,
                            categories = categories,
                            transactions = transactions,
                            deletedTransactions = deletedTransactions,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { viewModel.isDarkMode.value = it },
                            onAddWallet = { name, type, bal, acc, col ->
                                viewModel.addWallet(name, type, bal, acc, col)
                            },
                            onTransfer = { from, to, amt, notes ->
                                viewModel.transferWallet(from, to, amt, notes)
                            },
                            onAddGoal = { title, amt, date, icon, col, notes ->
                                viewModel.addSavingsGoal(title, amt, date, icon, col, notes)
                            },
                            onContributeGoal = { goalId, walletId, amt, title ->
                                viewModel.contributeToGoal(goalId, walletId, amt, title)
                            },
                            onDeleteGoal = { goalId ->
                                viewModel.deleteSavingsGoal(goalId)
                            },
                            onAddRecurring = { title, amt, type, catId, wId, freq, notes ->
                                viewModel.addRecurring(title, amt, type, catId, wId, freq, notes)
                            },
                            onProcessRecurringNow = { rec ->
                                viewModel.processRecurringNow(rec)
                            },
                            onToggleRecurringActive = { rec ->
                                viewModel.toggleRecurringActive(rec)
                            },
                            onDeleteRecurring = { id ->
                                viewModel.deleteRecurring(id)
                            },
                            onTriggerRecurringNotification = { rec ->
                                viewModel.triggerRecurringReminderNotification(context, rec)
                            },
                            onExportCsv = { ctx ->
                                viewModel.exportToCsv(ctx)
                            },
                            onRestoreDeletedTransaction = { id ->
                                viewModel.restoreTransaction(id)
                            },
                            onPermanentDeleteTransaction = { id ->
                                viewModel.permanentlyDeleteTransaction(id)
                            },
                            onClearTrash = {
                                viewModel.clearTrash()
                            },
                            isAutoSync = isAutoSync,
                            onToggleAutoSync = { enabled ->
                                viewModel.toggleAutoSync(context, enabled)
                            },
                            lastBackupTime = lastBackupTime,
                            isBackupInProgress = isBackupInProgress,
                            backupStatusMessage = backupStatusMessage,
                            userAccount = currentAccount,
                            isFirebaseActive = viewModel.isFirebaseActive(),
                            onOpenGoogleAccount = { showGoogleAccountDialog = true },
                            onTriggerManualBackup = {
                                viewModel.triggerManualBackup(context)
                            },
                            onRestoreFromCloud = { callback ->
                                viewModel.restoreFromCloud(context, callback)
                            },
                            onExportBackupJson = { ctx ->
                                viewModel.exportBackupJsonFile(ctx)
                            },
                            onRestoreFromJson = { json, callback ->
                                viewModel.restoreFromJsonString(json, callback)
                            },
                            onResetAllData = { callback ->
                                viewModel.resetToFreshState(callback)
                            },
                            initialTab = moreSubTab
                        )
                    }
                }
            }
        }

        // OCR Scanner Modal Sheet
        if (showScanSheet) {
            ScanReceiptSheet(
                isScanning = isScanning,
                scannedReceipt = scannedReceipt,
                scanMessage = scanMessage,
                onScanBitmap = { bitmap ->
                    viewModel.scanReceiptImage(context, bitmap)
                },
                onUseExtractedData = { parsed ->
                    showScanSheet = false
                    editingTransaction = null
                    pendingReceiptForAdd = parsed
                    showAddTransactionSheet = true
                },
                onDismiss = {
                    showScanSheet = false
                    viewModel.clearScannedReceipt()
                }
            )
        }

        // Add / Edit Transaction Sheet
        if (showAddTransactionSheet) {
            AddEditTransactionSheet(
                categories = categories,
                wallets = wallets,
                initialReceiptData = pendingReceiptForAdd,
                existingTransaction = editingTransaction,
                onSave = { id, title, amount, type, catId, walletId, dateMillis, notes, receiptPath, merchant, tags, editReason ->
                    if (id != null) {
                        viewModel.updateTransaction(
                            id = id,
                            title = title,
                            amount = amount,
                            type = type,
                            categoryId = catId,
                            walletId = walletId,
                            dateMillis = dateMillis,
                            notes = notes,
                            receiptImagePath = receiptPath,
                            merchantName = merchant,
                            tags = tags,
                            reason = editReason,
                            context = context
                        )
                    } else {
                        viewModel.addTransaction(
                            title = title,
                            amount = amount,
                            type = type,
                            categoryId = catId,
                            walletId = walletId,
                            dateMillis = dateMillis,
                            notes = notes,
                            receiptImagePath = receiptPath,
                            merchantName = merchant,
                            tags = tags,
                            context = context
                        )
                    }
                    showAddTransactionSheet = false
                    editingTransaction = null
                    pendingReceiptForAdd = null
                },
                onDismiss = {
                    showAddTransactionSheet = false
                    editingTransaction = null
                    pendingReceiptForAdd = null
                }
            )
        }

        // Transaction Detail Dialog
        selectedTransactionForDetail?.let { tx ->
            TransactionDetailDialog(
                transaction = tx,
                editHistoryFlow = viewModel.getEditHistoryForTransaction(tx.id),
                onEdit = {
                    editingTransaction = tx
                    pendingReceiptForAdd = null
                    selectedTransactionForDetail = null
                    showAddTransactionSheet = true
                },
                onRevertEdit = { history ->
                    viewModel.revertEdit(history)
                    selectedTransactionForDetail = null
                },
                onDismiss = { selectedTransactionForDetail = null },
                onDelete = {
                    viewModel.deleteTransaction(tx.id)
                    selectedTransactionForDetail = null
                }
            )
        }

        // Notifications Bottom Sheet
        if (showNotificationsSheet) {
            NotificationsSheet(
                notifications = notifications,
                unreadCount = unreadNotificationCount,
                onMarkAsRead = { notifId ->
                    viewModel.markNotificationAsRead(notifId)
                },
                onMarkAllAsRead = {
                    viewModel.markAllNotificationsAsRead()
                },
                onDeleteNotification = { notifId ->
                    viewModel.deleteNotification(notifId)
                },
                onClearAll = {
                    viewModel.clearAllNotifications()
                },
                onSendTestNotification = {
                    viewModel.sendTestSystemNotification(context)
                },
                onCreateCustomNotification = { title, message, type ->
                    viewModel.createAndSendCustomNotification(context, title, message, type)
                },
                onDismiss = { showNotificationsSheet = false }
            )
        }

        // Delete Confirmation Alert (from Long Click)
        transactionToDelete?.let { tx ->
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Pindahkan ke Sampah?") },
                text = { Text("Apakah Anda yakin ingin memindahkan \"${tx.title}\" senilai Rp ${tx.amount.toLong()} ke tempat sampah? Saldo dompet akan disesuaikan kembali.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(tx.id)
                            transactionToDelete = null
                        }
                    ) {
                        Text("Pindahkan ke Sampah", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Google Account & Sign-In Dialog
        if (showGoogleAccountDialog) {
            GoogleAccountDialog(
                authState = authState,
                savedWebClientId = viewModel.getWebClientId(),
                isFirebaseActive = viewModel.isFirebaseActive(),
                onSignInWithGoogle = { activityContext, clientId ->
                    viewModel.signInWithGoogle(activityContext, clientId)
                },
                onSignInQuick = { email, name ->
                    viewModel.signInQuick(email, name)
                },
                onSignOut = { activityContext ->
                    viewModel.signOut(activityContext)
                },
                onSaveWebClientId = { clientId ->
                    viewModel.saveWebClientId(clientId)
                },
                onDismiss = { showGoogleAccountDialog = false }
            )
        }
    }
}
