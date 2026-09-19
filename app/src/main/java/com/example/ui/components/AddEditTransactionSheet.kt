package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CategoryEntity
import com.example.data.model.WalletEntity
import com.example.network.ParsedReceipt
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.util.Formatters
import java.io.File

import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    categories: List<CategoryEntity>,
    wallets: List<WalletEntity>,
    previousTitles: List<String> = emptyList(),
    initialReceiptData: ParsedReceipt? = null,
    existingTransaction: com.example.data.local.TransactionWithDetails? = null,
    onSave: (
        id: Long?,
        title: String,
        amount: Double,
        type: String,
        categoryId: Long,
        walletId: Long,
        dateMillis: Long,
        notes: String,
        receiptImagePath: String?,
        merchantName: String,
        tags: String,
        editReason: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var transactionType by remember {
        mutableStateOf(
            existingTransaction?.type ?: if (initialReceiptData != null) "EXPENSE" else "EXPENSE"
        )
    }

    var amountText by remember {
        mutableStateOf(
            existingTransaction?.amount?.toLong()?.let { Formatters.formatNumberWithDots(it.toString()) }
                ?: if (initialReceiptData != null && initialReceiptData.amount > 0) {
                    Formatters.formatNumberWithDots(initialReceiptData.amount.toLong().toString())
                } else ""
        )
    }

    var titleText by remember {
        mutableStateOf(
            existingTransaction?.title ?: initialReceiptData?.merchantName ?: ""
        )
    }

    var notesText by remember {
        mutableStateOf(
            existingTransaction?.notes ?: initialReceiptData?.notes ?: ""
        )
    }

    var gestureStartTime by remember { mutableLongStateOf(0L) }
    var lastGestureDuration by remember { mutableLongStateOf(0L) }

    // Allow swipe down dismissal if gesture is sustained (~1 second) or fields are blank,
    // but reject quick accidental flicks under 1 second (< 800ms) to protect user input.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newState ->
            if (newState == SheetValue.Hidden) {
                val currentDuration = if (gestureStartTime > 0L) {
                    System.currentTimeMillis() - gestureStartTime
                } else {
                    lastGestureDuration
                }
                // Allow dismiss if sustained pull down >= 800ms (~1 second) OR no input has been entered
                val isFieldsEmpty = amountText.isBlank() && titleText.isBlank() && notesText.isBlank()
                isFieldsEmpty || currentDuration >= 800L
            } else {
                true
            }
        }
    )

    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    var merchantText by remember {
        mutableStateOf(
            existingTransaction?.merchantName ?: initialReceiptData?.merchantName ?: ""
        )
    }

    var receiptImagePath by remember {
        mutableStateOf(
            existingTransaction?.receiptImagePath ?: initialReceiptData?.receiptImagePath
        )
    }

    var dateMillis by remember {
        mutableLongStateOf(
            existingTransaction?.dateMillis ?: initialReceiptData?.dateMillis ?: System.currentTimeMillis()
        )
    }

    var editReasonText by remember {
        mutableStateOf(if (existingTransaction != null) "Koreksi nominal / detail transaksi" else "")
    }

    // Determine default category based on OCR or existing or first category
    val matchingCategory = remember(categories, initialReceiptData, existingTransaction, transactionType) {
        if (existingTransaction != null) {
            categories.find { it.id == existingTransaction.categoryId }
        } else if (initialReceiptData != null) {
            categories.find { it.name.contains(initialReceiptData.categoryName, ignoreCase = true) }
        } else {
            categories.filter { it.type == transactionType }.firstOrNull()
        }
    }

    var selectedCategoryId by remember {
        mutableLongStateOf(
            existingTransaction?.categoryId ?: matchingCategory?.id ?: categories.firstOrNull()?.id ?: 1L
        )
    }

    var selectedWalletId by remember {
        mutableLongStateOf(
            existingTransaction?.walletId ?: wallets.firstOrNull()?.id ?: 1L
        )
    }

    var targetWalletId by remember {
        mutableLongStateOf(
            existingTransaction?.toWalletId ?: wallets.getOrNull(1)?.id ?: 2L
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                if (gestureStartTime == 0L) {
                                    gestureStartTime = System.currentTimeMillis()
                                }
                            } else {
                                if (gestureStartTime > 0L) {
                                    lastGestureDuration = System.currentTimeMillis() - gestureStartTime
                                }
                                gestureStartTime = 0L
                            }
                        }
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                if (gestureStartTime == 0L) {
                                    gestureStartTime = System.currentTimeMillis()
                                }
                            } else {
                                if (gestureStartTime > 0L) {
                                    lastGestureDuration = System.currentTimeMillis() - gestureStartTime
                                }
                                gestureStartTime = 0L
                            }
                        }
                    }
                }
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        existingTransaction != null -> "Edit Transaksi"
                        initialReceiptData != null -> "Konfirmasi Catatan Struk"
                        else -> "Tambah Transaksi"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        val hasUnsavedChanges = amountText.isNotBlank() || titleText.isNotBlank() || notesText.isNotBlank()
                        if (hasUnsavedChanges && existingTransaction == null) {
                            showCancelConfirmDialog = true
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Type Segment Tabs
            val typeTabs = listOf("EXPENSE" to "Pengeluaran", "INCOME" to "Pemasukan", "TRANSFER" to "Transfer")
            PrimaryTabRow(
                selectedTabIndex = typeTabs.indexOfFirst { it.first == transactionType }.coerceAtLeast(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                typeTabs.forEach { (type, label) ->
                    Tab(
                        selected = transactionType == type,
                        onClick = {
                            transactionType = type
                            // Adjust category default
                            if (type != "TRANSFER") {
                                val firstInType = categories.firstOrNull { it.type == type }
                                if (firstInType != null) {
                                    selectedCategoryId = firstInType.id
                                }
                            }
                        },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (transactionType == type) FontWeight.Bold else FontWeight.Normal,
                                color = if (transactionType == type) {
                                    when(type) {
                                        "INCOME" -> IncomeGreen
                                        "EXPENSE" -> ExpenseRed
                                        else -> TransferBlue
                                    }
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nominal Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val cleanDigits = input.filter { it.isDigit() }
                    if (cleanDigits.length <= 15) {
                        amountText = Formatters.formatNumberWithDots(cleanDigits)
                    }
                },
                label = { Text("Jumlah Uang (Nominal)") },
                placeholder = { Text("0") },
                prefix = {
                    Text(
                        text = "Rp ",
                        fontWeight = FontWeight.Bold,
                        color = when(transactionType) {
                            "INCOME" -> IncomeGreen
                            "EXPENSE" -> ExpenseRed
                            else -> TransferBlue
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_amount"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title / Judul Transaksi with Smart Autocomplete Suggestions
            var expandedSuggestions by remember { mutableStateOf(false) }
            val matchingSuggestions = remember(titleText, previousTitles) {
                if (titleText.trim().isNotEmpty()) {
                    previousTitles
                        .filter {
                            it.contains(titleText.trim(), ignoreCase = true) &&
                            !it.equals(titleText.trim(), ignoreCase = true)
                        }
                        .distinct()
                        .take(6)
                } else {
                    emptyList()
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedSuggestions && matchingSuggestions.isNotEmpty(),
                onExpandedChange = { expandedSuggestions = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        expandedSuggestions = it.trim().isNotEmpty()
                    },
                    label = { Text(if (transactionType == "TRANSFER") "Keterangan Transfer" else "Judul / Keterangan Transaksi") },
                    placeholder = { Text("Ketik kata kunci: Gaji, Makan, Bensin...") },
                    singleLine = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                        .fillMaxWidth()
                        .testTag("input_transaction_title"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (matchingSuggestions.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedSuggestions,
                        onDismissRequest = { expandedSuggestions = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        matchingSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    titleText = suggestion
                                    expandedSuggestions = false
                                }
                            )
                        }
                    }
                }
            }

            // Category selector (Hidden for Transfer)
            if (transactionType != "TRANSFER") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Pilih Kategori",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val availableCategories = categories.filter { it.type == transactionType }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val catColor = Formatters.parseColor(cat.colorHex)

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Formatters.getCategoryIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallet Selection
            Text(
                text = if (transactionType == "TRANSFER") "Dari Dompet / Rekening" else "Pilih Dompet / Akun",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.forEach { wallet ->
                    val isSelected = selectedWalletId == wallet.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedWalletId = wallet.id },
                        label = {
                            Text("${wallet.name} (${Formatters.formatRupiah(wallet.balance)})")
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Target Wallet for Transfers
            if (transactionType == "TRANSFER") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ke Dompet / Rekening Tujuan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.filter { it.id != selectedWalletId }.forEach { wallet ->
                        val isSelected = targetWalletId == wallet.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { targetWalletId = wallet.id },
                            label = { Text(wallet.name) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Merchant / Toko (Optional)
            if (transactionType != "TRANSFER") {
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Tempat (Opsional)") },
                    placeholder = { Text("Contoh: McDonald's, Shell, Indomaret") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Catatan
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Catatan Tambahan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            // Reason for edit (if editing)
            if (existingTransaction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editReasonText,
                    onValueChange = { editReasonText = it },
                    label = { Text("Alasan Perubahan (Audit Log)") },
                    placeholder = { Text("Contoh: Salah ketik nominal struk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Attached Receipt Preview
            if (!receiptImagePath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val imgFile = File(receiptImagePath!!)
                            if (imgFile.exists()) {
                                AsyncImage(
                                    model = imgFile,
                                    contentDescription = "Struk",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Foto Struk Terlampir", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Akan disimpan sebagai referensi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(onClick = { receiptImagePath = null }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus Struk", tint = ExpenseRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val isValid = Formatters.parseAmount(amountText) > 0.0
            Button(
                onClick = {
                    val amount = Formatters.parseAmount(amountText)
                    val finalTitle = titleText.ifBlank {
                        if (transactionType == "TRANSFER") "Transfer Dompet" else "Transaksi"
                    }
                    onSave(
                        existingTransaction?.id,
                        finalTitle,
                        amount,
                        transactionType,
                        selectedCategoryId,
                        selectedWalletId,
                        dateMillis,
                        notesText,
                        receiptImagePath,
                        merchantText,
                        "",
                        editReasonText.ifBlank { "Pembaruan data transaksi" }
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_transaction"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingTransaction != null) "Perbarui Transaksi" else "Simpan Transaksi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Tutup Formulir?") },
            text = { Text("Data yang sedang Anda tulis belum disimpan. Apakah Anda yakin ingin keluar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Ya, Keluar", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Lanjutkan Menulis")
                }
            }
        )
    }
}
