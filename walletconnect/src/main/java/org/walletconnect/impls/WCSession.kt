package org.walletconnect.impls

import android.util.Log
import org.json.JSONObject
import org.walletconnect.Session
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.MethodCallException
import org.walletconnect.entity.SocketMessage
import org.walletconnect.entity.TransportError
import org.walletconnect.entity.WCConfig
import org.walletconnect.entity.WCSessionRequestResult
import org.walletconnect.entity.WCStatus
import org.walletconnect.toJSON
import org.walletconnect.tools.nullOnThrow
import org.walletconnect.tools.tryExec
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WCSession(
	private val config: WCConfig,
	private val payloadAdapter: Session.PayloadAdapter,
	private val sessionStore: WCSessionStore,
) : Session {
	// Non-persisted state
	private val transport = OkHttpTransport(
		config.bridge,
		config.proxy,
		statusHandler = ::handleStatus,
		messageHandler = ::handleMessage
	)

	private val subTopics = mutableSetOf<String>()

	private val requests: MutableMap<Long, (MethodCall.Response) -> Unit> = ConcurrentHashMap()
	private val sessionCallbacks: MutableSet<Session.Callback> =
		Collections.newSetFromMap(ConcurrentHashMap())

	init {
		subTopics.clear()
		val wcState = sessionStore.load(config.topic)
//		if (wcState != null) {
//			chainId = wcState.chainId
//			dApp = wcState.dApp
//
//			sessionId.set(wcState.dApp.id)
//			localApp = wcState.peerApp
//		} else {
//			localApp = clientPeer
//		}
//		storeSession()
	}

	override fun addCallback(cb: Session.Callback) {
		sessionCallbacks.add(cb)
	}

	override fun removeCallback(cb: Session.Callback) {
		sessionCallbacks.remove(cb)
	}

	override fun clearCallbacks() {
		sessionCallbacks.clear()
	}

	fun propagateToCallbacks(action: Session.Callback.() -> Unit) {
		sessionCallbacks.forEach { callback ->
			try {
				callback.action()
			} catch (t: Throwable) {
				// If error propagation fails, don't try again
				nullOnThrow { callback.onStatus(WCStatus.Error(t)) }
			}
		}
	}

	fun connect(): Boolean {
		return transport.connect()
	}

	private fun handleStatus(status: WCStatus) {
		propagateToCallbacks {
			onStatus(
				when (status) {
					is WCStatus.Error -> WCStatus.Error(
						TransportError(
							status.throwable
						)
					)
					else -> {
						Log.d(WalletConnect.TAG, "unknown status type.$status")
						status
					}
				}
			)
		}
	}

	private fun handleMessage(text: String) {
		tryExec({
			val json = JSONObject(text)
			val topic = json.optString("topic")
			val type = json.optString("type")
			val payload = json.optString("payload")

			if (WalletConnect.DEBUG_LOG) {
				Log.d(WalletConnect.TAG, "receive message: $json")
			}

			if (topic.isNullOrEmpty()) {
				Log.d(WalletConnect.TAG, "topic is null or empty.$topic")
				return@tryExec
			}
			if (type.isNullOrEmpty()) {
				Log.d(WalletConnect.TAG, "topic is null or empty.$type")
				return@tryExec
			}
			if (payload.isNullOrEmpty()) {
				Log.d(WalletConnect.TAG, "payload is null or empty.$payload")
				return@tryExec
			}

			val wcMessage = SocketMessage(topic = topic, type = type, payload = payload)
			if (wcMessage.type != "pub") {
				if (WalletConnect.DEBUG_LOG) {
					Log.d(
						WalletConnect.TAG, "pub message type: $text"
					)
				}
				return@tryExec
			}

			val data: String = try {
				payloadAdapter.decrypt(wcMessage.payload, config.key)
			} catch (e: Exception) {
				e.printStackTrace()
				handlePayloadError(e)
				return@tryExec
			}

			dispatchMessage(wcMessage, data)
		}, { error ->
			handlePayloadError(error)
			if (WalletConnect.DEBUG_LOG) {
				Log.d(WalletConnect.TAG, "handleMessage error: $error")
			}
		})
	}

	private fun dispatchMessage(wcMessage: SocketMessage, data: String) {

		subTopic(wcMessage.topic)

		val result = JSONObject(data)
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "dispatchMessage topic:${wcMessage.topic} result: $result")
		}

		val id = result.optLong("id")
		val callback = requests[id]
		if (callback != null) {
			val resp = MethodCall.Response(id = id, result = result, error = null)
			callback(resp)
			requests.remove(id)
		} else {
			Log.d(WalletConnect.TAG, "dispatchMessage callback is null")
		}

//		var accountToCheck: String? = null
//		when (data) {
//			is MethodCall.SessionRequest -> {
//				dappInfo = data.peer
//				storeSession()
//			}
//			is MethodCall.SessionUpdate -> {
//				if (!data.approved) {
//					endSession()
//				}
//			}
//			is MethodCall.SendTransaction -> {
//				accountToCheck = data.from
//			}
//			is MethodCall.SignMessage -> {
//				accountToCheck = data.address
//			}
//			is MethodCall.Response -> {
//				val callback = requests[data.id] ?: return@tryExec
//				callback(data)
//			}
//			is MethodCall.Custom -> {
//			}
//			is MethodCall.PersonalSignMessage -> {
//			}
//			else -> {}
//		}
//
//		if (accountToCheck?.let
//			{ accountCheck(data.id(), it) } != false
//		) {
//			propagateToCallbacks { onMethodCall(data) }
//		}
	}

//	private fun accountCheck(id: Long, address: String): Boolean {
//		approvedAccounts?.find { it.equals(address, ignoreCase = true) } ?: run {
//			handlePayloadError(MethodCallException.InvalidAccount(id, address))
//			return false
//		}
//		return true
//	}

	private fun handlePayloadError(e: Exception) {
		propagateToCallbacks { WCStatus.Error(e) }
		(e as? MethodCallException)?.let {
			//rejectRequest(it.id, it.code, it.message ?: "Unknown error")
		}
	}

	private fun endSession() {
		sessionStore.remove(config.topic)
		propagateToCallbacks { onStatus(WCStatus.Closed) }
	}

	@Synchronized
	fun send(
		id: Long,
		msg: String,
		callback: ((MethodCall.Response) -> Unit)? = null
	): Boolean {

		val topic: String = config.topic
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "Sending prepare: $msg")
		}
		val payload: String = payloadAdapter.encrypt(msg, config.key)

		callback?.let {
			requests[id] = callback
		}

		val pubMessage = SocketMessage(topic, "pub", payload)
		transport.send(pubMessage.message())
		subTopic(topic)
		return true
	}

	private fun subTopic(topic: String) {
		subTopics.add(topic)
		val subMessage = SocketMessage(topic, "sub", "")
		transport.send(subMessage.message())
	}

	override fun kill() {

		clearCallbacks()
		requests.clear()
		subTopics.clear()

		val requestId = WalletConnect.createCallId()
		val sessionUpdate = MethodCall.SessionUpdate(
			id = requestId,
			approved = false,
			accounts = emptyList()
		)
		val message = sessionUpdate.toJSON().toString()
		send(
			id = requestId,
			msg = message
		)

		transport.close()
		endSession()
	}

}

interface WCSessionStore {
	fun load(id: String): WCSessionRequestResult?

	fun store(id: String, state: WCSessionRequestResult)

	fun remove(id: String)

	fun list(): List<WCSessionRequestResult>
}