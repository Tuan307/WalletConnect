package org.walletconnect.impls

import org.json.JSONArray
import org.json.JSONObject
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.PeerMeta
import org.walletconnect.entity.SessionParams
import org.walletconnect.entity.WCError
import java.security.SecureRandom

internal fun walletSafeRandomBytes(size: Int) =
	ByteArray(size).also { bytes -> SecureRandom().nextBytes(bytes) }

internal fun <T : Any> JSONArray.toList(): List<T> {
	val list = mutableListOf<T>()
	for (i in 0 until length()) {
		val value = get(i)
		@Suppress("UNCHECKED_CAST")
		list.add(value as T)
	}
	return list
}

fun JSONObject.toSessionRequest(): MethodCall.SessionRequest {
	val id = getLong("id")
	val params = getJSONArray("params")
	val first = params.getJSONObject(0)
	return MethodCall.SessionRequest(
		id,
		first.extractPeerData()
	)
}

fun JSONObject.toSessionUpdate(): MethodCall.SessionUpdate {
	val id = getLong("id")
	val params = getJSONArray("params")
	val first = params.getJSONObject(0)
	return MethodCall.SessionUpdate(
		id,
		first.extractSessionParams()
	)
}

fun JSONObject.extractSessionParams(): SessionParams {
	val approved = getBoolean("approved")
	val chainId = getLong("chainId")
	val accounts: List<String> = getJSONArray("accounts").toList()
	return SessionParams(
		approved,
		chainId,
		accounts,
		extractPeerData()
	)
}

fun JSONObject.extractPeerData(): PeerData {
	val peerId = getString("peerId")
	val peerMetaObj = getJSONObject("peerMeta")
	return PeerData(peerId, peerMetaObj.extractPeerMeta(), null)
}

fun JSONObject.extractPeerMeta(): PeerMeta {
	val description = getString("description")
	val url = getString("url")
	val name = getString("name")
	val icons: List<String> = optJSONArray("icons")?.toList() ?: emptyList()
	return PeerMeta(url, name, description, icons)
}

fun JSONObject.toSendTransaction(): MethodCall.SendTransaction {
	val id = getLong("id")

	val params = getJSONArray("params")
	val data = params.getJSONObject(0)

	val from = data.getString("from")
	val to = data.getString("to")
	val nonce = data.getString("nonce")
	val gasPrice = data.getString("gasPrice")

	// "gasLimit" was used in older versions of the library, kept here as a fallback for compatibility
	val gasLimit = data.optString("gas", data.optString("gasLimit"))
	val value = data.optString("value", "0x0")
	val txData = data.getString("data")

	return MethodCall.SendTransaction(
		id,
		from,
		to,
		nonce,
		gasPrice,
		gasLimit,
		value,
		txData
	)
}

fun JSONObject.toSignMessage(): MethodCall.SignMessage {
	val id = getLong("id")
	val params = getJSONArray("params")
	val address = params.getString(0)
	val message = params.getString(1)
	return MethodCall.SignMessage(id, address, message)
}

fun JSONObject.toCustom(): MethodCall.Custom {
	val id = getLong("id")
	val method = getString("method")
	val params: List<Any> = getJSONArray("params").toList()
	return MethodCall.Custom(id, method, params)
}

fun JSONObject.toResponse(): MethodCall.Response {
	val id = getLong("id")
	val result = opt("result")
	val error = optJSONObject("error")
	if (result == null && error == null) {
		throw IllegalArgumentException("no result or error")
	}
	return MethodCall.Response(
		id,
		result,
		error?.extractError()
	)
}

fun JSONObject?.extractError(): WCError {
	val code = this?.optLong("code", 0L) ?: 0L
	val message = this?.optString("message", "Unknown error") ?: "Unknown error"
	return WCError(code, message)
}

fun List<*>?.toJSONArray(): JSONArray {
	val jsonArray = JSONArray()
	this?.forEach { item ->
		jsonArray.put(item)
	}
	return jsonArray
}

fun MethodCall.Response.toJSON(): JSONObject {
	val json = JSONObject()
	json.put("id", id)
	json.put("jsonrpc", "2.0")
	result?.let {
		json.put("result", result)
	}
	error?.let {
		json.put("error", error.toJSON())
	}
	return json
}

fun WCError.toJSON(): JSONObject {
	val json = JSONObject()
	json.put("code", code)
	json.put("message", message)
	return json
}