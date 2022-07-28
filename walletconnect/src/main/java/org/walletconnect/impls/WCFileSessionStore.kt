package org.walletconnect.impls

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.walletconnect.WalletConnect
import org.walletconnect.entity.ClientMeta
import org.walletconnect.entity.WCSessionRequestResult
import org.walletconnect.security.SecurityStoreCompat
import java.util.concurrent.ConcurrentHashMap

class WCFileSessionStore(val context: Context) : WCSessionStore {

	private val currentStates: MutableMap<String, WCSessionRequestResult> =
		ConcurrentHashMap()

	init {
		val storeContent = SecurityStoreCompat.decrypt(context, "session-store")
		if (storeContent.isNotEmpty() && storeContent.isNotBlank()) {
			val json = JSONObject(storeContent)
			val map = mutableMapOf<String, WCSessionRequestResult>()
			json.keys().forEach { key ->
				try {
					val item = json.getJSONObject(key)
					if (item.has("peerId")
						&& item.has("peerMeta")
						&& item.has("approved")
						&& item.has("chainId")
						&& item.has("accounts")
					) {

						val peerId = json.getString("peerId")
						val peerMeta = ClientMeta.fromJSON(json.getJSONObject("peerMeta"))

						val approved = item.optBoolean("approved", false)
						val chainId = json.getLong("chainId")
						val accounts = json.getJSONArray("accounts").toList<String>()
						val networkId = json.getLong("networkId")

						val sessionParams = WCSessionRequestResult(
							peerId = peerId,
							peerMeta = peerMeta,
							approved = approved,
							chainId = chainId,
							accounts = accounts,
							networkId = networkId,
						)
						map[key] = sessionParams
						Log.d(WalletConnect.TAG, "add wallet state $key")
					} else {
						Log.d(WalletConnect.TAG, "ignore wallet state $key")
					}
				} catch (throwable: Throwable) {
					Log.d(WalletConnect.TAG, "json key ${json.getJSONObject(key)}")
					throwable.printStackTrace()
				}
			}
			currentStates.putAll(map)
		}
	}

	override fun load(id: String): WCSessionRequestResult? = currentStates[id]

	override fun store(id: String, state: WCSessionRequestResult) {
		currentStates[id] = state
		writeToFile()
	}

	override fun remove(id: String) {
		currentStates.remove(id)
		writeToFile()
	}

	override fun list(): List<WCSessionRequestResult> = currentStates.values.toList()

	private fun writeToFile() {
		val json = JSONObject()
		currentStates.entries.forEach { entry ->
			val key: String = entry.key
			val value: WCSessionRequestResult = entry.value
			if (value.accounts.isNotEmpty()) {
				val item = JSONObject()
				item.put("peerId", value.peerId)
				item.put("peerMeta", value.peerMeta.toJSON())
				item.put("approved", value.approved)
				item.put("chainId", value.chainId)
				item.put("accounts", value.accounts.toJSONArray())
				item.put("networkId", value.networkId)
				json.put(key, item)
			}
		}
		SecurityStoreCompat.encrypt(context, "session-store", json.toString())
	}
}