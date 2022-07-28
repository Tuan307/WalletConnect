package org.walletconnect.samples

import android.widget.Toast
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall

/**
 * [personal_sign](https://docs.walletconnect.com/json-rpc-api-methods/ethereum#eth_sendtransaction)
 */

fun MainActivity.sendTransaction(walletConnect: WalletConnect) {
	val address = MainActivity.accounts.firstOrNull()
	if (address.isNullOrEmpty()) {
		Toast.makeText(this, "accounts is null", Toast.LENGTH_SHORT).show()
		return
	}

	val message = MethodCall.SendTransaction(
		id = WalletConnect.createCallId(),
		from = address,
		to = "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
		nonce = 279.toBigInteger(),
		gas = 30400.toBigInteger(),
		gasPrice = 10000000000000.toBigInteger(),
		value = 2441406250.toBigInteger(),
		data = "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675"
	)

	//WalletConnect.sendMessage(call = message)
}