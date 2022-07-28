package org.walletconnect.entity

import org.json.JSONObject
import org.walletconnect.impls.toJSONArray
import org.walletconnect.impls.toList

data class ClientMeta(
	val name: String,
	val url: String,
	val description: String,
	val icons: List<String>? = null,
) {
	fun toJSON(): JSONObject {
		val json = JSONObject()
		json.put("description", description)
		json.put("url", url)
		json.put("name", name)
		json.put("icons", icons.toJSONArray())
		return json
	}

	companion object {
		fun fromJSON(json: JSONObject) = ClientMeta(
			url = json.getString("url"),
			name = json.getString("name"),
			description = json.getString("description"),
			icons = json.getJSONArray("icons").toList()
		)
	}
}