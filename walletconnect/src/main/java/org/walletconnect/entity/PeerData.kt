package org.walletconnect.entity

import org.json.JSONObject

data class PeerData(val peerId: String, val peerMeta: PeerMeta, val chainId: Long?) {

	fun toJSON(): JSONObject {
		val json = JSONObject()
		json.put("peerId", peerId)
		json.put("peerMeta", peerMeta.toJSON())
		// TODO chainId
		return json
	}

	companion object {
		fun fromJSON(json: JSONObject) = PeerData(
			peerId = json.getString("peerId"),
			peerMeta = PeerMeta.fromJSON(json.getJSONObject("meta")),
			chainId = null
		)
	}
}