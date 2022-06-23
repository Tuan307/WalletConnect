package org.walletconnect.entity

import org.json.JSONObject

data class PeerData(val id: String, val meta: ClientMeta?) {

	fun toJSON(): JSONObject {
		val json = JSONObject()
		json.put("peerId", id)
		meta?.let {
			json.put("peerMeta", meta.toJSON())
		}
		return json
	}

	companion object {
		fun fromJSON(json: JSONObject): PeerData {
			val peerId = json.getString("peerId")
			val peerMeta = json.getJSONObject("peerMeta")
			return PeerData(
				id = peerId,
				meta = ClientMeta.fromJSON(peerMeta)
			)
		}
	}
}