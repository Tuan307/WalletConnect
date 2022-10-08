package org.walletconnect

import android.util.Log
import org.json.JSONObject
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.WCStatus

class CallbackAdapter(private val connectApproved: (JSONObject?) -> Unit) : Session.Callback {

    override fun onStatus(status: WCStatus) {
        when (status) {
            WCStatus.Approved -> {
                val accounts = WalletConnect.approvedResult()
                connectApproved.invoke(accounts)
                if (WalletConnect.DEBUG_LOG) {
                    Log.d(WalletConnect.TAG, "Approved: $accounts")
                }
            }
            WCStatus.Closed -> {
                if (WalletConnect.DEBUG_LOG) {
                    Log.d(WalletConnect.TAG, "Disconnected")
                }
            }
            WCStatus.Connected -> {
                if (WalletConnect.DEBUG_LOG) {
                    Log.d(WalletConnect.TAG, "Connected")
                }
            }
            WCStatus.Disconnected -> {
                if (WalletConnect.DEBUG_LOG) {
                    Log.d(WalletConnect.TAG, "Disconnected")
                }
            }
            is WCStatus.Error -> {
                status.throwable.printStackTrace()
                if (WalletConnect.DEBUG_LOG) {
                    Log.e(WalletConnect.TAG, "Error")
                }
            }
        }
    }

    override fun onMethodCall(call: MethodCall) {
        if (WalletConnect.DEBUG_LOG) {
            Log.d(WalletConnect.TAG, "id:${call.id()}")
        }
    }
}
