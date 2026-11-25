package com.aistudio.ecotobacco.kfzqw.ui

import com.aistudio.ecotobacco.kfzqw.BuildConfig

object Routes {
    val APP_VERSION: String = BuildConfig.VERSION_NAME
    const val HOME = "home"
    const val TRANSACTION_ENTRY = "transaction_entry"
    const val PRODUCT_MASTER = "product_master"
    const val REPORTS = "reports"
    const val REPORTS_THIS_MONTH = "reports_this_month"
    const val DEBUG_MENU = "debug_menu"
    const val SETTINGS = "settings"
    const val PROCUREMENT_PLANNING = "procurement_planning"
    const val RECEIPT_SCANNER = "receipt_scanner"

    // Legacy aliases
    const val PRODUCTS = PRODUCT_MASTER
    const val TRANSACTIONS = TRANSACTION_ENTRY
    const val PROCUREMENT = PROCUREMENT_PLANNING
}
