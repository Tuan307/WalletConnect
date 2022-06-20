package org.walletconnect.entity

import org.json.JSONObject

data class PeerData(val id: String, val meta: PeerMeta?) {

    fun toJSON(): JSONObject {
        val json = JSONObject()
        json.put("peerId", id)
        meta?.let {
            json.put("peerMeta", meta.toJSON())
        }
        return json
    }

    companion object {
        fun fromJSON(json: JSONObject) = PeerData(
            id = json.getString("peerId"),
            meta = PeerMeta.fromJSON(json.getJSONObject("meta"))
        )
    }
}
