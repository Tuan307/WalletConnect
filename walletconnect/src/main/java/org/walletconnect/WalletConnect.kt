package org.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.walletconnect.entity.ClientMeta
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.WCConfig
import org.walletconnect.entity.WCError
import org.walletconnect.entity.WCSessionRequestResult
import org.walletconnect.entity.WCStatus
import org.walletconnect.impls.WCFileSessionStore
import org.walletconnect.impls.WCPayloadAdapter
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore
import org.walletconnect.impls.toList
import java.util.concurrent.atomic.AtomicInteger

class WalletConnect(
	private val wcConfig: WCConfig,
	private val sessionStore: WCSessionStore,
	private val peerApp: PeerData,
	private val wcSession: WCSession,
) {

	companion object {

		const val TAG: String = "WalletConnect"

		const val VERSION = 1
		const val DEBUG_LOG = true
		const val TEST_BRIDGE = "https://bridge.bitea.one"
		const val WC_BRIDGE = "https://bridge.walletconnect.org"
		const val GNOSIS_BRIDGE = "https://safe-walletconnect.gnosis.io"

		private val callId = AtomicInteger(0)

		fun createCallId() = callId.incrementAndGet().toLong()

		@Throws(WCException::class)
		fun connect(
			wcConfig: WCConfig,
			peerData: PeerData
		): WalletConnect {

			val context = wcConfig.context
			if (wcConfig.specialApp.isNotEmpty()) {
				if (!isSupportWallet(context, wcConfig.specialApp)) {
					throw WCException("WCConfig's specialApp is not support wallet")
				}
			}

			val store = WCFileSessionStore(context)

			val session = WCSession(
				config = wcConfig,
				payloadAdapter = WCPayloadAdapter(),
				sessionStore = store,
			)

			return WalletConnect(
				wcConfig = wcConfig,
				sessionStore = store,
				peerApp = peerData,
				wcSession = session
			).apply {
				connect()
			}
		}


		fun isSupportWallet(context: Context, packageName: String): Boolean {
			val queryIntent = Intent(Intent.ACTION_MAIN)
			queryIntent.setPackage(packageName)
			val resolveInfo = context.packageManager.resolveActivity(queryIntent, 0)
			return resolveInfo != null
		}
	}

	private fun connect(): Boolean {
		return wcSession.connect()
	}

	private fun callWalletApp() {
		val context = wcConfig.context
		val specialApp = wcConfig.specialApp
		val uri = wcConfig.toWCUri()

		if (DEBUG_LOG) {
			Log.d(TAG, "wc:{topic...}@{version...}?bridge={url...}&key={key...}")
			Log.d(TAG, "wc uri:$uri")
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
		wcSession.kill()
	}

	/**
	 * [wc_sessionRequest](https://docs.walletconnect.com/tech-spec#session-request)
	 */
	fun sessionRequest(result: (WCSessionRequestResult) -> Unit, error: (WCError) -> Unit) {

		val requestId = createCallId()
		val message = MethodCall.SessionRequest(
			id = requestId,
			peer = peerApp,
		).toJSON().toString()

		wcSession.send(
			id = requestId,
			msg = message,
			callback = { resp ->
				if (resp.error != null) {
					error.invoke(resp.error)
				} else if (resp.result == null) {
					error.invoke(WCError(-1, "wc_sessionRequest failed, dapp result is null."))
				} else {
					val json = resp.result
					val accounts = json.optJSONArray("accounts")
					if (accounts == null || accounts.length() < 1) {
						error.invoke(WCError(-1, "wc_sessionRequest failed, accounts:${accounts}."))
					} else {
						val approved = json.optBoolean("approved", false)
						val chainId = json.optLong("chainId", 0)
						val networkId = json.optLong("networkId", 0)

						val accounts = json.getJSONArray("accounts").toList<String>()
						val peerId = json.getString("peerId")

						val peerMeta = json.getJSONObject("peerMeta")
						val name = peerMeta.getString("name")
						val url = peerMeta.getString("url")
						val description = peerMeta.getString("description")
						val icons = peerMeta.getJSONArray("icons")

						val dAppInfo =
							ClientMeta(
								name = name,
								url = url,
								description = description,
								icons = icons.toList(),
							)

						val sessionParams = WCSessionRequestResult(
							peerId = peerId,
							peerMeta = dAppInfo,
							approved = approved,
							chainId = chainId,
							accounts = accounts,
							networkId = networkId,
						)

						sessionStore.store(wcConfig.topic, sessionParams)

						wcSession.propagateToCallbacks {
							onStatus(
								if (approved) {
									WCStatus.Approved
								} else {
									WCStatus.Closed
								}
							)
						}
						result.invoke(sessionParams)
					}
				}
			}
		)

		callWalletApp()
	}


	/**
	 * [wc_sessionUpdate](https://docs.walletconnect.com/tech-spec#session-update)
	 */
	fun wcSessionUpdate(approved: Boolean, accounts: List<String>) {

	}

	/**
	 * [personal_sign](https://docs.walletconnect.com/json-rpc-api-methods/ethereum#personal_sign)
	 */
	fun personalSign(address: String, message: String) {
		val requestId = createCallId()
		val msg = MethodCall.PersonalSignMessage(
			id = requestId,
			address = address,
			message = message,
		).toJSON().toString()
		wcSession.send(
			id = requestId,
			msg = msg
		)
		callWalletApp()
	}

	/**
	 * [eth_sign](https://docs.walletconnect.com/json-rpc-api-methods/ethereum#eth_sign)
	 */
	fun ethSign(address: String, message: String) {
		val requestId = createCallId()
		val msg = MethodCall.SignMessage(
			id = requestId,
			address = address,
			message = message,
		).toJSON().toString()
		wcSession.send(
			id = requestId,
			msg = msg
		)
		callWalletApp()
	}
}