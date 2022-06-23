package org.walletconnect.samples

import android.os.SystemClock
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall

/**
 * [wc_sessionUpdate](https://docs.walletconnect.com/tech-spec#session-update)
 */
fun MainActivity.wcSessionUpdate() {
	val chainId = SystemClock.elapsedRealtime()
	WalletConnect.sendMessage(
		MethodCall.SessionUpdate(
			id = chainId,
			chainId = chainId,
			approved = true,
			accounts = MainActivity.accounts
		)
	)

}