package org.walletconnect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.util.Log
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.WCConfig
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCFileSessionStore
import org.walletconnect.impls.WCPayloadAdapter
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class WalletConnect(val peerApp: PeerData) {

    companion object {

        const val TAG: String = "WalletConnect"

        const val VERSION = 1
        const val DEBUG_LOG = true
        const val WC_BRIDGE = "https://bridge.walletconnect.org"
        const val GNOSIS_BRIDGE = "https://safe-walletconnect.gnosis.io"
        const val TEST_BRIDGE = "https://bridge.bitea.one"

        private val callId = AtomicInteger(0)

        fun createCallId() = callId.incrementAndGet().toLong()

        fun isSupportWallet(context: Context, packageName: String): Boolean {
            val queryIntent = Intent(Intent.ACTION_MAIN)
            queryIntent.setPackage(packageName)
            val resolveInfo = context.packageManager.resolveActivity(queryIntent, 0)
            return resolveInfo != null
        }

        fun connect(
            context: Context,
            config: WCConfig,
            peerApp: PeerData,
            storage: WCSessionStore? = null,
            specialApp: String = "",
            callback: Session.Callback
        ): WalletConnect {
            if (specialApp.isNotEmpty()) {
                if (!isSupportWallet(context, specialApp)) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    try {
                        intent.data =
                            Uri.parse("https://play.google.com/store/apps/details?id=$specialApp")
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        throw IllegalArgumentException("specialApp is not support wallet")
                    }
                }
            }

            val walletConnect = WalletConnect(peerApp = peerApp)
            walletConnect.storage = storage ?: WCFileSessionStore(
                File(context.cacheDir, "session_store.json")
                    .apply { createNewFile() }
            )

            walletConnect.release()
            walletConnect.newSession(config)
            walletConnect.session?.addCallback(callback)
            walletConnect.specialApp = specialApp
            return walletConnect
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    private var session: WCSession? = null
    private var specialApp: String? = null
    private var storage: WCSessionStore? = null

    fun openWalletApp(context: Context, appName: String) {
        if (isSupportWallet(context, appName)) {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = session?.config?.toWCUri()
            uri?.let {
                intent.data = Uri.parse(uri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (specialApp?.isNotEmpty() == true) {
                intent.setPackage(specialApp)
            }
            context.startActivity(intent)
        } else {
            //do nothing
        }
    }

    fun release() {
        session?.clearCallbacks()
        session?.kill()
    }

    private fun newSession(config: WCConfig) {
        session = WCSession(
            config,
            WCPayloadAdapter(),
            storage!!,
            OkHttpTransport.Builder(client),
            peerApp.peerMeta
        )
        session!!.offer()
    }

    fun approvedResult(): JSONObject? {
        return session?.approvedResult()
    }

    fun sendMessage(call: MethodCall, callback: ((MethodCall.Response) -> Unit)) {
        session?.performMethodCall(call = call) { response ->
            callback(response)
        }
    }
}