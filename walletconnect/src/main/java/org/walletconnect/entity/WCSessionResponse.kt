package org.walletconnect.entity


open class WCSessionRequest(
	val id: Long,
	val jsonrpc: String,
	val method: String,
	val params: List<Any>
)

open class WCSessionResponse<T>(val id: Long, val jsonrpc: String, val result: T)