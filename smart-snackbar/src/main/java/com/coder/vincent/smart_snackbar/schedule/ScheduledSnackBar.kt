package com.coder.vincent.smart_snackbar.schedule

import com.coder.vincent.smart_snackbar.SnackBarConfig

interface ScheduledSnackBar {
    fun config(): SnackBarConfig
    fun applyNewConfig(config: SnackBarConfig): ScheduledSnackBar
    fun show()
    fun dismiss()
    fun isShowing(): Boolean
}