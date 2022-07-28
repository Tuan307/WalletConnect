package org.walletconnect.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PeerData(
	@Json(name = "peerId")
	val peerId: String,
	@Json(name = "peerMeta")
	val peerMeta: ClientMeta,
	@Json(name = "chainId")
	val chainId: Int?,
)