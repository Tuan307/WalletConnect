package org.walletconnect.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClientMeta(
	@Json(name = "name")
	val name: String,
	@Json(name = "url")
	val url: String,
	@Json(name = "description")
	val description: String,
	@Json(name = "icons")
	val icons: List<String>? = null,
)