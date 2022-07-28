package org.walletconnect.impls

import android.util.Log
import org.json.JSONObject
import org.walletconnect.Session
import org.walletconnect.WCMethod
import org.walletconnect.WCMoshi
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.MethodCallException
import org.walletconnect.entity.PeerData
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
import java.util.concurrent.atomic.AtomicReference

class WCSession(
	private val wcConfig: WCConfig,
	private val payloadAdapter: Session.PayloadAdapter,
	private val sessionStore: WCSessionStore,
	private val peerData: PeerData,
) : Session {

	private val transport = OkHttpTransport(
		wcConfig.bridge,
		wcConfig.proxy,
		statusHandler = ::handleStatus,
		messageHandler = ::handleMessage
	)

	private val requests: MutableMap<Long, (MethodCall.Response) -> Unit> = ConcurrentHashMap()
	private val sessionCallbacks: MutableSet<Session.Callback> =
		Collections.newSetFromMap(ConcurrentHashMap())
	private val subscribers = mutableSetOf<String>()
	private val handshakeId = AtomicReference<String>()

	init {
		val wcState = sessionStore.load(wcConfig.topic)
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

	override fun kill() {

		val requestId = WalletConnect.createCallId()
		val sessionUpdate = MethodCall.SessionUpdate(
			id = requestId,
			approved = false,
			accounts = emptyList()
		)
		val message = sessionUpdate.toJSON().toString()
		send(
			id = requestId,
			jsonRpc = message
		)

		close()

		endSession()
	}

	fun connect(): Boolean {
		return transport.connect()
	}

	private fun handleStatus(status: WCStatus) {
		if (status == WCStatus.Connected) {
			// The Session.topic channel is used to listen session request messages only.
			//subscribe(wcConfig.topic)
			// The peerId channel is used to listen to all messages sent to this httpClient.
			subscribe(peerData.peerId)
		}
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


			if (type != "pub") {
				if (WalletConnect.DEBUG_LOG) {
					Log.d(
						WalletConnect.TAG, "pub message type: $text"
					)
				}
				return@tryExec
			}

			val decryptPayload: String = try {
				payloadAdapter.decrypt(payload, wcConfig.key)
			} catch (e: Exception) {
				e.printStackTrace()
				handlePayloadError(e)
				return@tryExec
			}
			val wcMessage = SocketMessage(topic = topic, type = type, payload = decryptPayload)
			dispatchMessage(wcMessage)
		}, { error ->
			handlePayloadError(error)
			if (WalletConnect.DEBUG_LOG) {
				Log.d(WalletConnect.TAG, "handleMessage error: $error")
			}
		})
	}

	private fun dispatchMessage(wcMessage: SocketMessage) {

		if (WalletConnect.DEBUG_LOG) {
			val message = WCMoshi.moshi.adapter(SocketMessage::class.java).toJson(wcMessage)
			Log.d(WalletConnect.TAG, "dispatchMessage message:$message")
		}

		val result = JSONObject(wcMessage.payload)
		val method = result.optString("method")
		if (method.isNullOrEmpty()) {
			val id = result.optLong("id")
			val callback = requests[id]
			if (callback != null) {
				val resp = MethodCall.Response(id = id, result = result, error = null)
				callback(resp)
				requests.remove(id)
			} else {
				Log.d(WalletConnect.TAG, "dispatchMessage callback is null")
			}
		} else {
			// do custom handling
			handleRequestMessage(method, wcMessage)
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

	private fun handleRequestMessage(method: String, wcMessage: SocketMessage) {
		when (method) {
			WCMethod.SESSION_REQUEST.value -> {
				val json = JSONObject(wcMessage.payload)
				val params = json.optJSONArray("params")
				if (params != null && params.length() > 0) {
					val peer = params.getJSONObject(0)
					if (peer != null && peer.has("peerId")) {
						val peerId = peer.getString("peerId")
						handshakeId.set(peerId)
						subscribe(peerId)
					}
				}
			}
			WCMethod.SESSION_UPDATE.value -> {}
			WCMethod.ETH_SIGN.value -> {}
			WCMethod.ETH_PERSONAL_SIGN.value -> {}
			WCMethod.ETH_SIGN_TYPE_DATA.value -> {}
			WCMethod.ETH_SIGN_TRANSACTION.value -> {}
			WCMethod.ETH_SEND_TRANSACTION.value -> {}
			WCMethod.GET_ACCOUNTS.value -> {}
			WCMethod.SIGN_TRANSACTION.value -> {}
			WCMethod.WALLET_SWITCH_NETWORK.value -> {}
			else -> {
				Log.d(WalletConnect.TAG, "handleRequestMessage support: $method")
			}
		}
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
		sessionStore.remove(wcConfig.topic)
		propagateToCallbacks { onStatus(WCStatus.Closed) }
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

	@Synchronized
	fun send(
		id: Long,
		jsonRpc: String,
		callback: ((MethodCall.Response) -> Unit)? = null
	): Boolean {
		if (!transport.isConnected()) {
			connect()
		}

		val topic: String = handshakeId.get() ?: wcConfig.topic
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "Sending prepare: $jsonRpc")
		}
		val payload: String = payloadAdapter.encrypt(jsonRpc, wcConfig.key)

		callback?.let {
			requests[id] = callback
		}

		val pubMessage = SocketMessage(topic, "pub", payload)
		val message = WCMoshi.moshi.adapter(SocketMessage::class.java).toJson(pubMessage)
		transport.send(message)

		return true
	}

	fun subscribe(topic: String) {
		if (subscribers.contains(topic)) {
			return
		}
		subscribers.add(topic)
		val subMessage = SocketMessage(topic, "sub", "")
		val message = WCMoshi.moshi.adapter(SocketMessage::class.java).toJson(subMessage)
		transport.send(message)
	}

	fun close() {
		clearCallbacks()
		requests.clear()
		subscribers.clear()
		transport.close()
	}

	fun sessionId(): String? {
		return handshakeId.get()
	}
}

interface WCSessionStore {
	fun load(id: String): WCSessionRequestResult?

	fun store(id: String, state: WCSessionRequestResult)

	fun remove(id: String)

	fun list(): List<WCSessionRequestResult>
}