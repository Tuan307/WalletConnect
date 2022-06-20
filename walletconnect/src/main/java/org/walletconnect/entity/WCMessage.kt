package org.walletconnect.entity

import org.json.JSONObject

data class WCMessage(
    val topic: String,
    val type: String,
    val payload: String
) {
    companion object {
        fun fromJson(json: JSONObject): WCMessage {
            return WCMessage(
                json.getString("topic"),
                json.getString("type"),
                json.getString("payload")
            )
        }
    }

    fun toJSON(): JSONObject {
        val json = JSONObject()
        json.put("topic", topic)
        json.put("type", type)
        json.put("payload", payload)
        return json
    }
}
