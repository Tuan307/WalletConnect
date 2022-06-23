package org.walletconnect.samples

import org.walletconnect.WalletConnect
import org.walletconnect.entity.ClientMeta


fun MainActivity.wcSetup() {
	WalletConnect.setCustomSessionStore(applicationContext)
	WalletConnect.setCustomPeerMeta(
		ClientMeta(
			url = "https://example.com",
			name = getString(R.string.app_name),
			description = "WalletConnect Sample App",
		)
	)
}