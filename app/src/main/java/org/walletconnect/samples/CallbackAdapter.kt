package org.walletconnect.samples

import android.util.Log
import org.walletconnect.Session
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.WCStatus

abstract class CallbackAdapter : Session.Callback {

	abstract fun onStatusChanged(status: WCStatus)

	override fun onStatus(status: WCStatus) {
		when (status) {
			WCStatus.Approved -> {
				if (WalletConnect.DEBUG_LOG) {
					Log.d(WalletConnect.TAG, "Approved")
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
		onStatusChanged(status)
	}


	override fun onMethodCall(call: MethodCall) {
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "id:${call.id()}")
		}
	}
}