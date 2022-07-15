package org.walletconnect.samples

import android.widget.Toast
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall

/**
 * [personal_sign](https://docs.walletconnect.com/json-rpc-api-methods/ethereum#personal_sign)
 */

// TODO
fun MainActivity.personalSign() {
	val address = MainActivity.accounts.firstOrNull()
	if (address.isNullOrEmpty()) {
		Toast.makeText(this, "accounts is null", Toast.LENGTH_SHORT).show()
		return
	}
	val message = MethodCall.PersonalSignMessage(
		id = WalletConnect.createCallId(),
		address = address,
		message = "0xdeadbeaf",
	)

	WalletConnect.sendMessage(call = message)
	//WalletConnect.callWalletApp(this, "")
}