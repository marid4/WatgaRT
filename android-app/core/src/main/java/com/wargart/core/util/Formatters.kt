package com.wargart.core.util

import java.text.NumberFormat
import java.util.Locale

object Formatters {
    private val idrFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    fun rupiah(amount: Double): String = idrFormat.format(amount)

    fun isAdminRole(roleCode: String): Boolean =
        roleCode in setOf("SUPER_ADMIN", "ADMIN_RW", "ADMIN_RT")
}
