package org.walletconnect.samples

import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall

/**
 * [wc_sessionUpdate](https://docs.walletconnect.com/tech-spec#session-update)
 */
fun MainActivity.wcSessionUpdate() {
	WalletConnect.sendMessage(
		MethodCall.SessionUpdate(
			id = WalletConnect.createCallId(),
			approved = true,
			accounts = MainActivity.accounts
		)
	)
}