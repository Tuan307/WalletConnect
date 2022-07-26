package org.walletconnect.entity

import org.json.JSONObject
import java.net.Proxy

data class WCConfig(
	val topic: String,
	val bridge: String,
	val key: String,
	val protocol: String = "wc",
	val version: Int = 1,
	val proxy: Proxy? = null
) {

	fun toJSON(): JSONObject {
		val json = JSONObject()
		json.put("topic", topic)
		json.put("bridge", bridge)
		json.put("key", key)
		json.put("protocol", protocol)
		json.put("version", version)
		return json
	}

	companion object {
		fun fromJSON(config: JSONObject): WCConfig {
			val topic = config.getString("topic")
			val bridge = config.getString("bridge")
			val key = config.getString("key")
			val protocol = config.optString("protocol", "wc")
			val version = config.optInt("version", 1)
			return WCConfig(topic, bridge, key, protocol, version)
		}
	}
}