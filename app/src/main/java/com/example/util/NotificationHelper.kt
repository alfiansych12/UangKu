package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.util.Formatters

object NotificationHelper {
    const val CHANNEL_BUDGET_ALERTS = "budget_alerts_channel"
    const val CHANNEL_RECURRING_REMINDERS = "recurring_reminders_channel"
    const val CHANNEL_BACKUP_SYNC = "backup_sync_channel"

    private var channelsCreated = false

    fun createNotificationChannels(context: Context) {
        if (channelsCreated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET_ALERTS,
            "Peringatan Anggaran (Budget)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi saat pengeluaran mendekati atau melampaui batas anggaran"
            enableVibration(true)
        }

        val recurringChannel = NotificationChannel(
            CHANNEL_RECURRING_REMINDERS,
            "Pengingat Transaksi Rutin",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pengingat tagihan dan transaksi rutin yang akan jatuh tempo"
        }

        val backupChannel = NotificationChannel(
            CHANNEL_BACKUP_SYNC,
            "Cloud Backup & Sinkronisasi",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Status sinkronisasi dan pencadangan data keuangan"
        }

        notificationManager.createNotificationChannels(listOf(budgetChannel, recurringChannel, backupChannel))
        channelsCreated = true
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendBudgetAlert(
        context: Context,
        categoryName: String,
        spent: Double,
        limit: Double,
        percentage: Float
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val isExceeded = percentage >= 1.0f
        val title = if (isExceeded) {
            "⚠️ Anggaran Terlampaui: $categoryName"
        } else {
            "⚡ Peringatan Anggaran: $categoryName"
        }

        val content = if (isExceeded) {
            "Pengeluaran $categoryName telah melebihi batas (${(percentage * 100).toInt()}%). Terpakai: ${Formatters.formatRupiah(spent)} dari limit ${Formatters.formatRupiah(limit)}."
        } else {
            "Pengeluaran $categoryName mencapai ${(percentage * 100).toInt()}% dari batas bulanan. Sisa anggaran: ${Formatters.formatRupiah((limit - spent).coerceAtLeast(0.0))}."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                ("budget_$categoryName".hashCode()),
                notification
            )
        } catch (_: SecurityException) {}
    }

    fun sendRecurringReminder(
        context: Context,
        title: String,
        amount: Double,
        dueDaysText: String
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val message = "Tagihan \"$title\" sebesar ${Formatters.formatRupiah(amount)} $dueDaysText. Pastikan saldo dompet mencukupi."

        val notification = NotificationCompat.Builder(context, CHANNEL_RECURRING_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📅 Pengingat Tagihan: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                ("recurring_$title".hashCode()),
                notification
            )
        } catch (_: SecurityException) {}
    }

    fun sendBackupNotification(
        context: Context,
        isSuccess: Boolean,
        message: String
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val title = if (isSuccess) "☁️ Backup Cloud Berhasil" else "❌ Gagal Menyimpan Backup"

        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP_SYNC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                9999,
                notification
            )
        } catch (_: SecurityException) {}
    }
}
