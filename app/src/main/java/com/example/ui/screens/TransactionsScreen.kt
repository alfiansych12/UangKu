package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionWithDetails
import com.example.data.model.CategoryEntity
import com.example.data.model.WalletEntity
import com.example.ui.components.TransactionItemCard
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionWithDetails>,
    categories: List<CategoryEntity>,
    wallets: List<WalletEntity> = emptyList(),
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    searchHistory: List<String> = emptyList(),
    selectedTypeFilter: String,
    onTypeFilterChange: (String) -> Unit,
    selectedCategoryId: Long?,
    onCategoryFilterChange: (Long?) -> Unit,
    selectedWalletId: Long? = null,
    onWalletFilterChange: (Long?) -> Unit = {},
    selectedTimeRange: String,
    onTimeRangeChange: (String) -> Unit,
    minAmount: Double? = null,
    maxAmount: Double? = null,
    onMinAmountChange: (Double?) -> Unit = {},
    onMaxAmountChange: (Double?) -> Unit = {},
    sortBy: String,
    onSortChange: (String) -> Unit,
    activeFilterCount: Int = 0,
    onResetFilters: () -> Unit = {},
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onTransactionLongClick: (TransactionWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedFilterSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Transaksi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Cari, filter, dan telusuri transaksi Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Advanced Filter Trigger Button
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge { Text("$activeFilterCount") }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { showAdvancedFilterSheet = true },
                        modifier = Modifier.testTag("btn_open_filters")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter Lanjutan",
                            tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari judul, merchant, catatan, nominal...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Cari")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_transactions"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Quick Search History Suggestion Chips
        if (searchHistory.isNotEmpty() && searchQuery.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Populer:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    searchHistory.take(5).forEach { historyTag ->
                        FilterChip(
                            selected = false,
                            onClick = { onSearchChange(historyTag) },
                            label = { Text(historyTag, fontSize = 11.sp) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }

        // Type Filter Chips (Semua, Pengeluaran, Pemasukan, Transfer)
        item {
            val types = listOf(
                "ALL" to "Semua",
                "EXPENSE" to "Pengeluaran",
                "INCOME" to "Pemasukan",
                "TRANSFER" to "Transfer"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                types.forEach { (typeKey, typeLabel) ->
                    val isSelected = selectedTypeFilter == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTypeFilterChange(typeKey) },
                        label = { Text(typeLabel) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // Time Range & Sorting Quick Chips
        item {
            val timeRanges = listOf(
                "ALL" to "Semua Waktu",
                "THIS_MONTH" to "Bulan Ini",
                "LAST_7_DAYS" to "7 Hari Terakhir",
                "LAST_30_DAYS" to "30 Hari Terakhir"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeRanges.forEach { (tKey, tLabel) ->
                    val isSelected = selectedTimeRange == tKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTimeRangeChange(tKey) },
                        label = { Text(tLabel) },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Active Filter Feedback Bar
        if (activeFilterCount > 0 || searchQuery.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ditemukan ${transactions.size} transaksi ($activeFilterCount filter aktif)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = onResetFilters,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Filter", fontSize = 11.sp)
                    }
                }
            }
        }

        // Transactions List
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Tidak ada transaksi yang cocok",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Coba ubah kata kunci pencarian atau reset filter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    onClick = { onTransactionClick(tx) },
                    onLongClick = { onTransactionLongClick(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Modal Bottom Sheet for Advanced Filtering
    if (showAdvancedFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdvancedFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Transaksi Lengkap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showAdvancedFilterSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dompet / Rekening Filter
                if (wallets.isNotEmpty()) {
                    Text(
                        text = "Berdasarkan Dompet / Rekening",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedWalletId == null,
                            onClick = { onWalletFilterChange(null) },
                            label = { Text("Semua Dompet") }
                        )
                        wallets.forEach { w ->
                            FilterChip(
                                selected = selectedWalletId == w.id,
                                onClick = { onWalletFilterChange(if (selectedWalletId == w.id) null else w.id) },
                                label = { Text(w.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Kategori Filter
                Text(
                    text = "Berdasarkan Kategori",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("Semua Kategori") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { onCategoryFilterChange(if (selectedCategoryId == cat.id) null else cat.id) },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Urutkan (Sorting)
                Text(
                    text = "Urutan Data",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                val sortOptions = listOf(
                    "NEWEST" to "Terbaru",
                    "OLDEST" to "Terlama",
                    "HIGHEST" to "Nominal Terbesar",
                    "LOWEST" to "Nominal Terkecil"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortOptions.forEach { (sKey, sLabel) ->
                        FilterChip(
                            selected = sortBy == sKey,
                            onClick = { onSortChange(sKey) },
                            label = { Text(sLabel) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rentang Nominal
                Text(
                    text = "Rentang Nominal (Rp)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = minAmount?.toLong()?.toString() ?: "",
                        onValueChange = { onMinAmountChange(it.toDoubleOrNull()) },
                        label = { Text("Min") },
                        placeholder = { Text("0") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxAmount?.toLong()?.toString() ?: "",
                        onValueChange = { onMaxAmountChange(it.toDoubleOrNull()) },
                        label = { Text("Maks") },
                        placeholder = { Text("Tanpa batas") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons (Terapkan & Reset)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetFilters()
                            showAdvancedFilterSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }

                    Button(
                        onClick = { showAdvancedFilterSheet = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Terapkan Filter")
                    }
                }
            }
        }
    }
}
