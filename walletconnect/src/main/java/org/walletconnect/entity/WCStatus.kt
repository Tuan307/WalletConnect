package org.walletconnect.entity

sealed class WCStatus {
    object Connected : WCStatus()
    object Disconnected : WCStatus()
    object Approved : WCStatus()
    object Closed : WCStatus()
    data class Error(val throwable: Throwable) : WCStatus()
}
