package org.walletconnect.entity

import org.json.JSONObject

data class WCState(
    val config: WCConfig,
    val clientData: PeerData,
    val peerData: PeerData?,
    val handshakeId: Long?,
    val currentKey: String,
    val approvedResult: JSONObject?,
    val chainId: Long?
) {

    fun toJSON(): JSONObject {
        val json = JSONObject()
        json.put("config", config.toJSON())
        json.put("clientData", clientData.toJSON())
        peerData?.let {
            json.put("peerData", peerData.toJSON())
        }
        json.put("handshakeId", handshakeId)
        json.put("currentKey", currentKey)
        approvedResult?.let {
            json.put("approvedResult", approvedResult)
        }
        json.put("chainId", chainId)
        return json
    }

    companion object {

        fun fromJSON(json: JSONObject): WCState {
            val handshakeId = json.getLong("handshakeId")
            val chainId = json.getLong("chainId")

            val config = WCConfig.fromJSON(json.getJSONObject("config"))
            val clientData = PeerData.fromJSON(json.getJSONObject("clientData"))

            val currentKey = json.getString("currentKey")
            val approvedResult = json.getJSONObject("approvedResult")

            val peerData =
                json.optJSONObject("peerData")?.let { PeerData.fromJSON(it) }

            return WCState(
                config = config,
                clientData = clientData,
                peerData = peerData,
                handshakeId = handshakeId,
                currentKey = currentKey,
                approvedResult = approvedResult,
                chainId = chainId
            )
        }
    }
}
