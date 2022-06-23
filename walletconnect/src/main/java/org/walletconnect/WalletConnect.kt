package org.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
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

object WalletConnect {

	const val TAG: String = "WalletConnect"
	const val VERSION = 1
	const val DEBUG_LOG = true
	const val WC_BRIDGE = "https://bridge.walletconnect.org"
	const val GNOSIS_BRIDGE = "https://safe-walletconnect.gnosis.io"

	private val client: OkHttpClient = OkHttpClient.Builder().build()

	private var session: Session? = null
	private var storage: WCSessionStore? = null
	private var clientPeer: PeerData? = null

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

		val intent = Intent(Intent.ACTION_VIEW)
		intent.data = Uri.parse(config.toWCUri())
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		if (specialApp.isNotEmpty()) {
			intent.setPackage(specialApp)
		}
		context.startActivity(intent)
	}

	fun release() {
		session?.clearCallbacks()
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

	fun approvedAccounts(): List<String> {
		return session?.approvedAccounts() ?: emptyList()
	}

	fun sendMessage(call: MethodCall) {
		session?.performMethodCall(call = call)
	}
}