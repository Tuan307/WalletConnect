package org.walletconnect.entity

import org.json.JSONObject

data class WCMessage(
	val topic: String,
	val type: String,
	val payload: String
) {
	fun toJSON(): JSONObject {
		val json = JSONObject()
		json.put("topic", topic)
		json.put("type", type)
		json.put("payload", payload)
		return json
	}
}