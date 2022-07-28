package org.walletconnect.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.walletconnect.WCMethod


@JsonClass(generateAdapter = true)
class WCSessionRequest<T>(
	@Json(name = "id")
	val id: Long,
	@Json(name = "jsonrpc")
	val jsonrpc: String,
	@Json(name = "method")
	val method: WCMethod,
	@Json(name = "params")
	val params: List<T>
)

open class WCSessionResponse<T>(
	@Json(name = "id")
	val id: Long,
	@Json(name = "jsonrpc")
	val jsonrpc: String,
	@Json(name = "result")
	val result: T
)