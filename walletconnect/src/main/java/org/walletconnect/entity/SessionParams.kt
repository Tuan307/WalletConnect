package org.walletconnect.entity

import org.json.JSONObject
import org.walletconnect.impls.toJSONArray
import org.walletconnect.impls.toList

data class SessionParams(
	val approved: Boolean,
	val chainId: Long,
	val accounts: List<String>?,
	val peerData: PeerData = PeerData("", null),
) {

	fun toJSON(): JSONObject {
		val json = JSONObject()
		json.put("approved", approved)
		json.put("chainId", chainId)
		json.put("accounts", accounts.toJSONArray())
		json.put("peerData", peerData.toJSON())
		return json
	}

	companion object {
		fun fromJSON(json: JSONObject) = SessionParams(
			approved = json.getBoolean("approved"),
			chainId = json.getLong("chainId"),
			accounts = json.optJSONArray("accounts")?.toList(),
			peerData = PeerData.fromJSON(json)
		)
	}
}