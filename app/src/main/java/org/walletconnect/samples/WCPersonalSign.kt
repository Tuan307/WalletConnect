package org.walletconnect.samples

import android.util.Log
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall

/**
 * [personal_sign](https://docs.walletconnect.com/json-rpc-api-methods/ethereum#eth_sendtransaction)
 */

fun WalletConnect.personalSign(
    address: String,
    message: String,
    callback: ((MethodCall.Response) -> Unit)
) {
    val call = MethodCall.PersonalSignMessage(
        id = WalletConnect.createCallId(), address = address, message = message
    )
    sendMessage(call) { response ->
        callback(response)
        Log.d("CheckSign", response.result.toString())
    }
}

