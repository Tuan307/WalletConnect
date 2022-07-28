package org.walletconnect.impls

import android.util.Log
import org.json.JSONObject
import org.walletconnect.Session
import org.walletconnect.WalletConnect
import org.walletconnect.tools.decode
import org.walletconnect.tools.toNoPrefixHexString
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class WCPayloadAdapter : Session.PayloadAdapter {

	override fun encrypt(data: String, key: String): String {

//		if (WalletConnect.DEBUG_LOG) {
//			Log.d(WalletConnect.TAG, "send key:$key jsonrpc:$data")
//		}

		val bytesData: ByteArray = data.toByteArray()
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

	override fun decrypt(payload: String, key: String): String {

		val json = JSONObject(payload)

//		if (WalletConnect.DEBUG_LOG) {
//			Log.d(WalletConnect.TAG, "parse:$json")
//		}

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
					"key:$key hmac:$hmac result:${hmacResult.toNoPrefixHexString()}"
				)
			}
			//throw IllegalArgumentException("Invalid hmac")
		}

		val outBuf = cipher.doFinal(encryptedData)
		return String(outBuf)
	}

}