package org.walletconnect.samples

import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall

/**
 * [personal_sign](https://docs.walletconnect.com/json-rpc-api-methods/ethereum#eth_sendtransaction)
 */

fun WalletConnect.personalSign(address: String, message: String) {
	val call = MethodCall.PersonalSignMessage(
		id = WalletConnect.createCallId(), address = address, message = message
	)
	sendMessage(call)

}