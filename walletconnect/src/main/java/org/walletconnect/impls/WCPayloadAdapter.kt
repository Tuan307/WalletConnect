package org.walletconnect.impls

import android.util.Log
import org.json.JSONObject
import org.walletconnect.Session
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall
import org.walletconnect.tools.decode
import org.walletconnect.tools.toHexString
import org.walletconnect.tools.toNoPrefixHexString
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class WCPayloadAdapter : Session.PayloadAdapter {

	override fun prepare(data: MethodCall, key: String): String {
		val param = data.toJSON()

		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "key:$key param:$param")
		}

		val bytesData: ByteArray = param.toString().toByteArray()
		val hexKey = decode(key)

		val secretKeySpec = SecretKeySpec(hexKey, "AES")
		val iv = walletSafeRandomBytes(16)
		val ivSpec = IvParameterSpec(iv)

		val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)

		val outBuf = cipher.doFinal(bytesData)

		val mac: Mac = Mac.getInstance("HmacSHA256")
		mac.init(SecretKeySpec(hexKey, "HmacSHA256"))
		mac.update(outBuf)
		mac.update(iv)
		val hmacResult = mac.doFinal()

		val json = JSONObject()
		json.put("data", outBuf.toNoPrefixHexString())
		json.put("iv", iv.toNoPrefixHexString())
		json.put("hmac", hmacResult.toNoPrefixHexString())
		return json.toString()
	}

	override fun parse(payload: String, key: String): MethodCall {
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "key:$key payload:$payload")
		}

		val json = JSONObject(payload)

		val data: String = json.getString("data")
		val iv: String = json.getString("iv")
		val hmac: String = json.getString("hmac")

		val hexKey = decode(key)
		val ivByte = decode(iv)

		val secretKeySpec = SecretKeySpec(hexKey, "AES")
		val ivSpec = IvParameterSpec(ivByte)

		val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)

		val encryptedData = decode(data)

		val mac: Mac = Mac.getInstance("HmacSHA256")
		mac.init(SecretKeySpec(hexKey, "HmacSHA256"))
		mac.update(encryptedData)
		mac.update(ivByte)
		val hmacResult = mac.doFinal()

		if (hmac != hmacResult.toNoPrefixHexString()) {
			if (WalletConnect.DEBUG_LOG) {
				Log.d(
					WalletConnect.TAG,
					"key:$key dapp hmac:$hmac result:${hmacResult.toNoPrefixHexString()}"
				)
			}
			// throw IllegalArgumentException("Invalid hmac")
		}

		val outBuf = cipher.doFinal(encryptedData)
		return outBuf.toMethodCall()
	}

	/**
	 * Convert FROM request bytes
	 */
	private fun ByteArray.toMethodCall(): MethodCall =
		String(this).let { text ->
			val json = JSONObject(text)
			val method = if (json.has("method")) {
				json.getString("method")
			} else {
				null
			}
			when (method) {
				"wc_sessionRequest" -> json.toSessionRequest()
				"wc_sessionUpdate" -> json.toSessionUpdate()
				"eth_sendTransaction" -> json.toSendTransaction()
				"eth_sign" -> json.toSignMessage()
				null -> json.toResponse()
				else -> json.toCustom()
			}
		}

	/**
	 * Convert INTO request bytes
	 */
	private fun MethodCall.toJSON(): JSONObject = when (this) {
		is MethodCall.SessionRequest -> {
			jsonRpc(id, "wc_sessionRequest", peer.toJSON())
		}
		is MethodCall.SessionUpdate -> {
			jsonRpc(id, "wc_sessionUpdate", params.toJSON())
		}
		is MethodCall.SendTransaction -> {
			val json = JSONObject()
			json.put("from", from)
			json.put("to", to)
			json.put("nonce", nonce)
			json.put("gasPrice", gasPrice)
			json.put("gasLimit", gasLimit)
			json.put("value", value)
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
				id,
				"eth_sign",
				address,
				message
			)
		}
		is MethodCall.Custom -> {
			jsonRpcWithList(
				id,
				method,
				params ?: emptyList<Any>()
			)
		}
		is MethodCall.PersonalSignMessage -> {
			jsonRpc(
				id,
				"personal_sign",
				message.toByteArray().toHexString(),
				address,
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
}