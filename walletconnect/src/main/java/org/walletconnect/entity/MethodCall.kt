package org.walletconnect.entity

import org.json.JSONObject
import java.math.BigInteger

sealed class MethodCall(private val internalId: Long) {

	fun id() = internalId

	data class SessionRequest(val id: Long, val peer: PeerData) : MethodCall(id)

	data class SessionUpdate(
		val id: Long,
		val approved: Boolean,
		val accounts: List<String>
	) : MethodCall(id)

	data class SendTransaction(
		val id: Long,
		val from: String,
		val to: String,
		val nonce: BigInteger = (-1).toBigInteger(),
		val gas: BigInteger = (-1).toBigInteger(),
		val gasPrice: BigInteger = (-1).toBigInteger(),
		val value: BigInteger = (-1).toBigInteger(),
		val data: String
	) : MethodCall(id)

	data class SignMessage(val id: Long, val address: String, val message: String) :
		MethodCall(id)

	data class Custom(val id: Long, val method: String, val params: List<*>?) : MethodCall(id)

	data class Response(val id: Long, val result: JSONObject?, val error: WCError? = null) :
		MethodCall(id)

	/**
	 * Signing method that allows transporting a message without hashing allowing the message to
	 * be display as a human readable text when UTF-8 encoded.
	 *
	 * @see <a href="https://geth.ethereum.org/docs/rpc/ns-personal#personal_sign">personal_sign</a>
	 *
	 * @param id a unique identifier for the transaction
	 * @param address 20 Bytes - address
	 * @param message message as a human readable text
	 */
	data class PersonalSignMessage(val id: Long, val address: String, val message: String) :
		MethodCall(id)
}