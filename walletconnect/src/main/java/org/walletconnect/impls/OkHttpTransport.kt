package org.walletconnect.impls

import android.annotation.SuppressLint
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.walletconnect.Session
import org.walletconnect.WalletConnect
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
	private val messageHandler: (String) -> Unit,
) : Session.Transport, WebSocketListener() {

	private val socketLock = Any()
	private var webSocket: WebSocket? = null
	private var connected: Boolean = false
	private val queue: Queue<String> = ConcurrentLinkedQueue()

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
			webSocket ?: run {
				connected = false
				val bridgeWS = serverUrl.replace("https://", "wss://").replace("http://", "ws://")
				webSocket = client.newWebSocket(
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

	override fun send(payload: String) {
		queue.offer(payload)
		drainQueue()
	}

	private fun drainQueue() {
		if (connected) {
			webSocket?.let { s ->
				when (val message = queue.poll()) {
					null -> {
						Log.d(WalletConnect.TAG, "queue is empty")
						return
					}
					else -> {
						tryExec({
							if (WalletConnect.DEBUG_LOG) {
								Log.d(WalletConnect.TAG, "Sending: $message")
							}
							s.send(message)
						}, { error ->
							statusHandler.invoke(WCStatus.Error(error))
						})
					}
				}
			} ?: run {
				Log.d(WalletConnect.TAG, "Socket is null")
			}
		} else {
			connect()
		}
	}

	override fun onOpen(webSocket: WebSocket, response: Response) {
		super.onOpen(webSocket, response)
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "WebSocket connected.")
		}
		connected = true

		drainQueue()
		statusHandler(WCStatus.Connected)
	}

	override fun onMessage(webSocket: WebSocket, text: String) {
		super.onMessage(webSocket, text)
		tryExec({
			messageHandler(text)
		}, { error ->
			statusHandler.invoke(WCStatus.Error(error))
		})
	}

	override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
		super.onFailure(webSocket, t, response)
		statusHandler(WCStatus.Error(t))
		close()
	}

	override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
		super.onClosed(webSocket, code, reason)
		close()
	}

	override fun close() {
		if (WalletConnect.DEBUG_LOG) {
			Log.d(WalletConnect.TAG, "WebSocket close.")
		}
		webSocket?.close(1000, null)
		webSocket = null
		connected = false
		statusHandler(WCStatus.Disconnected)
	}
}