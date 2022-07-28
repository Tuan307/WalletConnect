package org.walletconnect.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The communications are all relayed using WebSockets 'stringified' payloads with the following structure:
 *
 * @see <a href="https://docs.walletconnect.com/tech-spec#websocket-messages">personal_sign</a>
 *
 * The Bridge Server acts as pub/sub controller which guarantees published messages are always received by their subscribers.
 */
@JsonClass(generateAdapter = true)
data class SocketMessage(
	@Json(name = "topic")
	val topic: String,
	@Json(name = "type")
	val type: String,
	@Json(name = "payload")
	val payload: String
)