package com.aistudio.ecotobacco.kfzqw.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.aistudio.ecotobacco.kfzqw.MainActivity
import com.aistudio.ecotobacco.kfzqw.R
import com.aistudio.ecotobacco.kfzqw.data.local.AppDatabase
import com.aistudio.ecotobacco.kfzqw.ui.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TobaccoAppWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TobaccoAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private data class WidgetData(
        val totalKulakanAmount: Double = 0.0,
        val totalKulakanCount: Int = 0,
        val totalKulakanQty: Double = 0.0,
        val activePlansCount: Int = 0,
        val pendingItemsCount: Int = 0,
        val totalProductsCount: Int = 0,
        val lastUpdateTime: String? = null
    )

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            // Commit immediate initial layout with default/placeholder values
            val initialViews = createResponsiveRemoteViews(context, appWidgetManager, appWidgetId, null)
            appWidgetManager.updateAppWidget(appWidgetId, initialViews)

            // Asynchronously fetch live data and update metrics
            scope.launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val products = db.productDao().getAllProductsDirect()
                    val transactions = db.transactionDao().getAllTransactionsDirect().filter { it.isDeleted == 0 }
                    val plans = db.procurementDao().getAllPlansWithItemsDirect()

                    // Calculate this month's procurement / transactions
                    val calendar = Calendar.getInstance()
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)

                    val thisMonthTx = transactions.filter { tx ->
                        calendar.timeInMillis = tx.date
                        calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                    }

                    val totalKulakanAmount = thisMonthTx.sumOf { it.total }
                    val totalKulakanCount = thisMonthTx.size
                    val totalKulakanQty = thisMonthTx.sumOf { it.quantity }

                    val activePlansCount = plans.size
                    val pendingItemsCount = plans.sumOf { plan -> plan.items.count { !it.isBought } }
                    val totalProductsCount = products.size

                    val timeFormat = SimpleDateFormat("HH:mm 'WIB'", Locale.getDefault())
                    val lastUpdateTime = timeFormat.format(Date())

                    val data = WidgetData(
                        totalKulakanAmount = totalKulakanAmount,
                        totalKulakanCount = totalKulakanCount,
                        totalKulakanQty = totalKulakanQty,
                        activePlansCount = activePlansCount,
                        pendingItemsCount = pendingItemsCount,
                        totalProductsCount = totalProductsCount,
                        lastUpdateTime = lastUpdateTime
                    )

                    val updatedViews = createResponsiveRemoteViews(context, appWidgetManager, appWidgetId, data)
                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createResponsiveRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        data: WidgetData?
    ): RemoteViews {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ responsive multi-size layout mapping
            val viewMapping = mapOf(
                SizeF(70f, 60f) to buildRemoteViewsForLayout(context, R.layout.widget_tobacco_tiny, data),
                SizeF(110f, 40f) to buildRemoteViewsForLayout(context, R.layout.widget_tobacco_compact, data),
                SizeF(110f, 90f) to buildRemoteViewsForLayout(context, R.layout.widget_tobacco_medium, data),
                SizeF(200f, 90f) to buildRemoteViewsForLayout(context, R.layout.widget_tobacco_dashboard, data)
            )
            RemoteViews(viewMapping)
        } else {
            // Backward compatibility: select best layout based on current widget options
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
            val layoutId = when {
                minHeight < 80 && minWidth < 110 -> R.layout.widget_tobacco_tiny
                minHeight < 80 -> R.layout.widget_tobacco_compact
                minWidth < 200 || minHeight < 140 -> R.layout.widget_tobacco_medium
                else -> R.layout.widget_tobacco_dashboard
            }
            buildRemoteViewsForLayout(context, layoutId, data)
        }
    }

    private fun buildRemoteViewsForLayout(
        context: Context,
        layoutId: Int,
        data: WidgetData?
    ): RemoteViews {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val views = RemoteViews(context.packageName, layoutId)

        // 1. Month Badge (Compact format for small/medium to prevent wrapping)
        val monthFormat = if (layoutId == R.layout.widget_tobacco_dashboard) {
            SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        } else {
            SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
        }
        views.setTextViewText(R.id.widget_tv_month_badge, monthFormat.format(Date()))

        // Button Action 1: + Input Kulakan (QuickKulakanActivity floating dialog)
        val quickKulakanIntent = Intent(context, QuickKulakanActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val quickKulakanPendingIntent = PendingIntent.getActivity(
            context,
            REQ_QUICK_KULAKAN,
            quickKulakanIntent,
            flags
        )
        views.setOnClickPendingIntent(R.id.btn_widget_input_kulakan, quickKulakanPendingIntent)

        // Button Action 2: Bikin Rencana (QuickPlanActivity floating dialog)
        val quickPlanIntent = Intent(context, QuickPlanActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val quickPlanPendingIntent = PendingIntent.getActivity(
            context,
            REQ_QUICK_PLAN,
            quickPlanIntent,
            flags
        )
        views.setOnClickPendingIntent(R.id.btn_widget_bikin_rencana, quickPlanPendingIntent)

        // Refresh Button Action
        val refreshIntent = Intent(context, TobaccoAppWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_REFRESH,
            refreshIntent,
            flags
        )
        views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

        // Navigation actions
        views.setOnClickPendingIntent(
            R.id.card_widget_hero,
            createNavPendingIntent(context, Routes.REPORTS_THIS_MONTH, REQ_NAV_REPORTS, flags)
        )
        views.setOnClickPendingIntent(
            R.id.card_widget_plans,
            createNavPendingIntent(context, Routes.PROCUREMENT_PLANNING, REQ_NAV_PLANS, flags)
        )
        views.setOnClickPendingIntent(
            R.id.card_widget_stock,
            createNavPendingIntent(context, Routes.PRODUCT_MASTER, REQ_NAV_PRODUCTS, flags)
        )
        views.setOnClickPendingIntent(
            R.id.widget_header,
            createNavPendingIntent(context, Routes.HOME, REQ_NAV_HOME, flags)
        )

        // Populate dynamic data if available
        if (data != null) {
            views.setTextViewText(
                R.id.widget_tv_total_kulakan_amount,
                formatRupiah(data.totalKulakanAmount)
            )

            views.setTextViewText(
                R.id.widget_tv_kulakan_stats,
                when (layoutId) {
                    R.layout.widget_tobacco_dashboard -> {
                        "${data.totalKulakanCount} Transaksi Kulakan • ${formatDecimal(data.totalKulakanQty)} kg"
                    }
                    R.layout.widget_tobacco_tiny -> {
                        "${data.totalKulakanCount} Trx"
                    }
                    R.layout.widget_tobacco_compact -> {
                        "• ${data.totalKulakanCount} Trx"
                    }
                    else -> {
                        "${data.totalKulakanCount} Trx • ${formatDecimal(data.totalKulakanQty)} kg"
                    }
                }
            )

            views.setTextViewText(R.id.widget_tv_active_plans, "${data.activePlansCount} Rencana")
            views.setTextViewText(
                R.id.widget_tv_plan_items,
                if (data.pendingItemsCount > 0) "${data.pendingItemsCount} Item Belum Beli" else "Semua Terbeli"
            )
            views.setTextViewText(
                R.id.widget_tv_total_products,
                if (layoutId == R.layout.widget_tobacco_dashboard) "${data.totalProductsCount} Jenis Produk" else "${data.totalProductsCount} Produk"
            )

            if (data.lastUpdateTime != null) {
                views.setTextViewText(R.id.widget_tv_last_update, data.lastUpdateTime)
            }
        }

        return views
    }

    private fun createNavPendingIntent(
        context: Context,
        route: String,
        requestCode: Int,
        flags: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVIGATE_TO, route)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun formatRupiah(amount: Double): String {
        return "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.aistudio.ecotobacco.ACTION_REFRESH_WIDGET"
        const val EXTRA_NAVIGATE_TO = "navigate_to"

        private const val REQ_REFRESH = 101
        private const val REQ_NAV_HOME = 102
        private const val REQ_QUICK_KULAKAN = 103
        private const val REQ_QUICK_PLAN = 104
        private const val REQ_NAV_PRODUCTS = 105
        private const val REQ_NAV_PLANS = 106
        private const val REQ_NAV_REPORTS = 107

        fun notifyDataChanged(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, TobaccoAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds.isEmpty()) return

                val intent = Intent(context, TobaccoAppWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
