package org.walletconnect.entity

import org.json.JSONObject

/**
 * The communications are all relayed using WebSockets 'stringified' payloads with the following structure:
 *
 * @see <a href="https://docs.walletconnect.com/tech-spec#websocket-messages">personal_sign</a>
 *
 * The Bridge Server acts as pub/sub controller which guarantees published messages are always received by their subscribers.
 */
data class SocketMessage(
	val topic: String,
	val type: String,
	val payload: String
) {

	fun message(): String {
		val json = JSONObject()
		json.put("topic", topic)
		json.put("type", type)
		json.put("payload", payload)
		return json.toString()
	}
}