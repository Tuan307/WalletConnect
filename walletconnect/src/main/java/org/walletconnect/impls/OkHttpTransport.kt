package org.walletconnect.impls

import android.annotation.SuppressLint
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.walletconnect.Session
import org.walletconnect.WalletConnect
import org.walletconnect.entity.WCMessage
import org.walletconnect.entity.WCStatus
import org.walletconnect.tools.tryExec
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class OkHttpTransport(
	private val serverUrl: String,
	private val proxy: Proxy?,
	private val statusHandler: (WCStatus) -> Unit,
	private val messageHandler: (WCMessage) -> Unit,
) : Session.Transport, WebSocketListener() {

	private val socketLock = Any()
	private var socket: WebSocket? = null
	private var connected: Boolean = false
	private val queue: Queue<WCMessage> = ConcurrentLinkedQueue()

	private val trustAllCerts: Array<TrustManager> = arrayOf(
		@SuppressLint("CustomX509TrustManager")
		object : X509TrustManager {
			@SuppressLint("TrustAllX509TrustManager")
			override fun checkClientTrusted(
				chain: Array<out X509Certificate>?,
				authType: String?
			) {
			}

			@SuppressLint("TrustAllX509TrustManager")
			override fun checkServerTrusted(
				chain: Array<out X509Certificate>?,
				authType: String?
			) {
			}

			override fun getAcceptedIssuers(): Array<X509Certificate> {
				return arrayOf()
			}
		}
	)

	private val sslSocketFactory by lazy {
		// Install the all-trusting trust manager
		val sslContext: SSLContext = SSLContext.getInstance("SSL")
		sslContext.init(null, trustAllCerts, SecureRandom())
		// Create an ssl socket factory with our all-trusting manager
		// Create an ssl socket factory with our all-trusting manager
		sslContext.socketFactory
	}

	@SuppressLint("Deprecated")
	fun addProxy(builder: OkHttpClient.Builder) {
		if (proxy != null) {
			builder.sslSocketFactory(
				sslSocketFactory,
				trustAllCerts[0] as X509TrustManager
			)
			builder.proxy(proxy)
			builder.hostnameVerifier { _, _ -> true }
		}
	}

	private val client: OkHttpClient = OkHttpClient.Builder()
		.connectTimeout(10, TimeUnit.SECONDS)
		.apply {
			addProxy(this)
		}
		.build()

	override fun isConnected(): Boolean = connected

	override fun connect(): Boolean {
		synchronized(socketLock) {
			socket ?: run {
				connected = false
				val bridgeWS = serverUrl.replace("https://", "wss://").replace("http://", "ws://")
				socket = client.newWebSocket(
					Request
						.Builder()
						.url(bridgeWS)
						.build(), this
				)
				return true
			}
		}
		return false
	}

	override fun send(message: WCMessage) {
		queue.offer(message)
		drainQueue()
	}

	private fun drainQueue() {
		if (connected) {
			socket?.let { s ->
				queue.poll()?.let { message ->
					tryExec({
						val json = message.toJSON()
						if (WalletConnect.DEBUG_LOG) {
							Log.d(WalletConnect.TAG, "Sending: $json")
						}
						s.send(json.toString())
					}, { error ->
						statusHandler.invoke(WCStatus.Error(error))
					})
					drainQueue() // continue draining until there are no more messages
				} ?: run {
					Log.d(WalletConnect.TAG, "queue is empty")
				}
			} ?: run {
				Log.d(WalletConnect.TAG, "Socket is null")
			}
		} else {
			connect()
		}
	}

	override fun close() {
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "web socket close.")
		}
		socket?.close(1000, null)
	}

	override fun onOpen(webSocket: WebSocket, response: Response) {
		super.onOpen(webSocket, response)
		connected = true
		drainQueue()
		statusHandler(WCStatus.Connected)
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "web socket connected.")
		}
	}

	override fun onMessage(webSocket: WebSocket, text: String) {
		super.onMessage(webSocket, text)
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

			val message = WCMessage(topic = topic, type = type, payload = payload)
			messageHandler(message)
		}, { error ->
			statusHandler.invoke(WCStatus.Error(error))
		})
	}

	override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
		super.onFailure(webSocket, t, response)
		statusHandler(WCStatus.Error(t))
		disconnected()
	}

	override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
		super.onClosed(webSocket, code, reason)
		disconnected()
	}

	private fun disconnected() {
		socket = null
		connected = false
		statusHandler(WCStatus.Disconnected)
	}

	class Builder(private val proxy: Proxy?) :
		Session.Transport.Builder {
		override fun build(
			url: String,
			statusHandler: (WCStatus) -> Unit,
			messageHandler: (WCMessage) -> Unit
		): Session.Transport =
			OkHttpTransport(url, proxy, statusHandler, messageHandler)
	}
}