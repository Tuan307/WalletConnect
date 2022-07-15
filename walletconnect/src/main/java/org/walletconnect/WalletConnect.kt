package org.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import okhttp3.OkHttpClient
import org.walletconnect.entity.ClientMeta
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.WCConfig
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCFileSessionStore
import org.walletconnect.impls.WCPayloadAdapter
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object WalletConnect {

	const val TAG: String = "WalletConnect"
	const val VERSION = 1
	const val DEBUG_LOG = true
	const val WC_BRIDGE = "https://bridge.walletconnect.org"
	const val GNOSIS_BRIDGE = "https://safe-walletconnect.gnosis.io"

	private val client: OkHttpClient = OkHttpClient.Builder().build()

	private var session: WCSession? = null
	private var storage: WCSessionStore? = null
	private var clientPeer: PeerData? = null
	private val callId = AtomicInteger(0)

	fun createCallId() = callId.incrementAndGet().toLong()

	fun isSupportWallet(context: Context, packageName: String): Boolean {
		val queryIntent = Intent(Intent.ACTION_MAIN)
		queryIntent.setPackage(packageName)
		val resolveInfo = context.packageManager.resolveActivity(queryIntent, 0)
		return resolveInfo != null
	}

	fun setCustomPeerMeta(clientMeta: ClientMeta) {
		val clientId = UUID.randomUUID().toString()
		clientPeer = PeerData(clientId, clientMeta)
	}

	fun setCustomSessionStore(context: Context) {
		storage = WCFileSessionStore(
			File(context.cacheDir, "session_store.json")
				.apply { createNewFile() },
		)
	}

	fun connect(
		context: Context,
		config: WCConfig,
		specialApp: String = "",
		callback: Session.Callback
	) {
		// check parameters
		if (clientPeer == null) {
			throw IllegalArgumentException("peerMeta is null,call setCustomPeerMeta(Session.PeerMeta)")
		}

		if (storage == null) {
			throw IllegalArgumentException("storage is null,call setCustomSessionStore(Context)")
		}

		if (specialApp.isNotEmpty()) {
			if (!isSupportWallet(context, specialApp)) {
				throw IllegalArgumentException("specialApp is not support wallet")
			}
		}

		release()

		newSession(config)
		session?.addCallback(callback)

		callWalletApp(context, specialApp)
	}

	fun callWalletApp(
		context: Context,
		specialApp: String = ""
	) {
		val uri = session?.config?.toWCUri()
			?: throw IllegalArgumentException("session config uri is null")

		if (DEBUG_LOG) {
			Log.d(TAG, "wc:{topic...}@{version...}?bridge={url...}&key={key...}")
			Log.d(TAG, "wc uri:$uri")
			session?.config?.fromWCUri(uri)?.let { config ->
				Log.d(TAG, "wc config:${config.toJSON()}")
			}
		}

		val intent = Intent(Intent.ACTION_VIEW)
		intent.data = Uri.parse(uri)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		if (specialApp.isNotEmpty()) {
			intent.setPackage(specialApp)
		}
		context.startActivity(intent)
	}

	fun release() {
		session?.kill()
	}

	private fun newSession(config: WCConfig) {
		session = WCSession(
			config = config,
			payloadAdapter = WCPayloadAdapter(),
			sessionStore = storage!!,
			clientPeer = clientPeer!!,
			transportBuilder = OkHttpTransport.Builder(client),
		)
		session!!.offer()
	}

	fun chainId(): Long {
		if (session is WCSession) {
			return (session as WCSession).chainId ?: 0L
		}
		return 0L
	}

	fun approvedAccounts(): List<String> {
		return session?.approvedAccounts() ?: emptyList()
	}

	fun sendMessage(call: MethodCall) {
		session?.performMethodCall(call = call)
	}
}