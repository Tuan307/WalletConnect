package org.walletconnect

import android.util.Log
import org.json.JSONObject
import org.walletconnect.entity.MethodCall
import org.walletconnect.impls.toCustom
import org.walletconnect.impls.toJSON
import org.walletconnect.impls.toJSONArray
import org.walletconnect.impls.toList
import org.walletconnect.impls.toResponse
import org.walletconnect.impls.toSendTransaction
import org.walletconnect.impls.toSessionRequest
import org.walletconnect.impls.toSignMessage
import org.walletconnect.tools.biggerThan
import org.walletconnect.tools.encode2Hex

/**
 * Convert FROM request bytes
 */
fun ByteArray.toMethodCall(): MethodCall =
	String(this).let { text ->

		val json = JSONObject(text)

		Log.d(WalletConnect.TAG, "receive payload:$json")

		val method = if (json.has("method")) {
			json.getString("method")
		} else {
			null
		}
		when (method) {
			"wc_sessionRequest" -> json.toSessionRequest()
			"wc_sessionUpdate" -> {
				val id = json.getLong("id")
				val params = json.getJSONArray("params")
				val first = params.getJSONObject(0)
				val approved = first.getBoolean("approved")
				val accounts = first.getJSONArray("accounts")
				return MethodCall.SessionUpdate(
					id = id,
					approved = approved,
					accounts = accounts.toList(),
				)
			}
			"eth_sendTransaction" -> json.toSendTransaction()
			"eth_sign" -> json.toSignMessage()
			"personal_sign" -> {
				val id = json.getLong("id")
				val params = json.getJSONArray("params")
				val address = params.getString(0)
				val message = params.getString(1)
				return MethodCall.PersonalSignMessage(
					id = id,
					address = address,
					message = message,
				)
			}
			null -> json.toResponse()
			else -> json.toCustom()
		}
	}

/**
 * Convert INTO request bytes
 */
fun MethodCall.toJSON(): JSONObject = when (this) {
	is MethodCall.SessionRequest -> {
		jsonRpc(id, "wc_sessionRequest", peer.toJSON())
	}
	is MethodCall.SessionUpdate -> {
		val json = JSONObject()
		json.put("chainId", WalletConnect.chainId())
		json.put("approved", approved)
		json.put("accounts", accounts.toJSONArray())
		jsonRpc(id, "wc_sessionUpdate", json)
	}
	is MethodCall.SendTransaction -> {
		val json = JSONObject()
		json.put("from", from)
		json.put("to", to)
		// encode as hex string
		if (nonce.biggerThan(0)) {
			json.put("nonce", nonce.encode2Hex())
		}
		if (gasPrice.biggerThan(0)) {
			json.put("gasPrice", gasPrice.encode2Hex())
		}
		if (gas.biggerThan(0)) {
			json.put("gasLimit", gas.encode2Hex())
			json.put("gas", gas.encode2Hex())
		}
		if (value.biggerThan(0)) {
			json.put("value", value.encode2Hex())
		}
		json.put("data", data)
		jsonRpc(id, "eth_sendTransaction", json)
	}
	is MethodCall.Response -> {
		val json = JSONObject()
		json.put("id", id)
		json.put("jsonrpc", "2.0")
		result?.let {
			json.put("result", result)
		}
		error?.let {
			json.put("error", error.toJSON())
		}
		json
	}
	is MethodCall.SignMessage -> {
		jsonRpc(
			id, "eth_sign", address, message
		)
	}
	is MethodCall.Custom -> {
		jsonRpcWithList(
			id, method, params ?: emptyList<Any>()
		)
	}
	is MethodCall.PersonalSignMessage -> {
		jsonRpc(
			id, "personal_sign", message, address
		)
	}
}

private fun jsonRpc(id: Long, method: String, vararg params: Any) =
	jsonRpcWithList(id, method, params.asList())

private fun jsonRpcWithList(id: Long, method: String, params: List<*>): JSONObject {
	val json = JSONObject()
	json.put("id", id)
	json.put("jsonrpc", "2.0")
	json.put("method", method)
	json.put("params", params.toJSONArray())
	return json
}