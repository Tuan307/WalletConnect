package org.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONObject
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

		const val JSONRPC_VERSION = "2.0"
		const val WS_CLOSE_NORMAL = 1000

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
				wcConfig = wcConfig,
				payloadAdapter = WCPayloadAdapter(),
				sessionStore = store,
				peerData = peerData,
			)

			return WalletConnect(
				wcConfig = wcConfig,
				sessionStore = store,
				peerApp = peerData,
				wcSession = session
			)
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
	 * <pre>
	 * interface WCSessionRequestRequest {
	 *      id: number;
	 *      jsonrpc: "2.0";
	 *      method: "wc_sessionRequest";
	 *      params: [
	 *          {
	 *              peerId: string;
	 *              peerMeta: ClientMeta;
	 *              chainId?: number | null;
	 *          }
	 *      ];
	 * }
	 *
	 * interface WCSessionRequestResponse {
	 *      id: number;
	 *      jsonrpc: "2.0";
	 *      result: {
	 *          peerId: string;
	 *          peerMeta: ClientMeta;
	 *          approved: boolean;
	 *          chainId: number;
	 * 		    accounts: string[];
	 * 	    };
	 * }
	 *
	 * </pre>
	 */
	fun sessionRequest(callResult: (WCSessionRequestResult) -> Unit, error: (WCError) -> Unit) {

		val requestId = createCallId()

		val peerData = PeerData(
			peerId = peerApp.peerId,
			peerMeta = peerApp.peerMeta,
			chainId = peerApp.chainId
		)
		val text = WCMoshi.moshi.adapter(PeerData::class.java).toJson(peerData)
		val jsonRpc = jsonRpc(
			id = requestId, method = WCMethod.SESSION_REQUEST.value, JSONObject(text)
		)

		wcSession.send(
			id = requestId,
			jsonRpc = jsonRpc,
			callback = { resp ->
				val result = extractResponse(resp = resp, error = error)
				if (result is JSONObject) {
					val sessionRequestResult =
						WCMoshi.moshi.adapter(WCSessionRequestResult::class.java)
							.fromJson(result.toString())
					if (sessionRequestResult == null) {
						error.invoke(
							WCError(
								-3,
								"wc_sessionRequest failed, payload's result parse failed."
							)
						)
					} else {
						sessionStore.store(wcConfig.topic, sessionRequestResult)
						wcSession.propagateToCallbacks {
							onStatus(
								if (sessionRequestResult.approved) {
									WCStatus.Approved
								} else {
									WCStatus.Closed
								}
							)
						}
						callResult.invoke(sessionRequestResult)
					}
				} else {
					error.invoke(
						WCError(
							-5,
							"wc_sessionRequest failed, result format is not json.$result"
						)
					)
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
	 *
	 * Signing method that allows transporting a message without hashing allowing the message to
	 * be display as a human readable text when UTF-8 encoded.
	 *
	 * @see <a href="https://geth.ethereum.org/docs/rpc/ns-personal#personal_sign">personal_sign</a>
	 *
	 * @param id a unique identifier for the transaction
	 * @param message message as a human readable text
	 * @param address 20 Bytes - address
	 */
	fun personalSign(
		message: String,
		address: String,
		signResult: (String) -> Unit,
		error: (WCError) -> Unit
	) {
		val requestId = createCallId()
		val jsonRpc = jsonRpc(
			id = requestId, method = WCMethod.ETH_PERSONAL_SIGN.value, message, address
		)
		wcSession.send(
			id = requestId,
			jsonRpc = jsonRpc,
			callback = { resp ->
				val result = extractResponse(resp = resp, error = error)
				if (result is String) {
					signResult.invoke(result)
				} else {
					error.invoke(
						WCError(
							-5,
							"personal_sign failed, result format is not string.$result"
						)
					)
				}
			}
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
		).toJSON()
		wcSession.send(
			id = requestId,
			jsonRpc = msg
		)
		callWalletApp()
	}

	private fun extractResponse(resp: MethodCall.Response, error: (WCError) -> Unit): Any? {
		if (resp.error != null) {
			error.invoke(resp.error)
		} else if (resp.result == null) {
			error.invoke(WCError(-1, "wc_sessionRequest failed, dapp result is null."))
		} else {
			val json = resp.result
			val result = json.opt("result")
			if (result == null) {
				error.invoke(
					WCError(
						-2,
						"wc_sessionRequest failed, payload's result is null."
					)
				)
			} else {
				return result
			}
		}
		return null
	}
}