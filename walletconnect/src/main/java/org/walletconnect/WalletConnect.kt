package org.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.PeerMeta
import org.walletconnect.entity.WCConfig
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCFileSessionStore
import org.walletconnect.impls.WCPayloadAdapter
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

object WalletConnect {

    const val TAG: String = "WalletConnect"
    const val VERSION = 1
    const val DEBUG_LOG = true
    const val WC_BRIDGE = "https://bridge.walletconnect.org"
    const val GNOSIS_BRIDGE = "https://safe-walletconnect.gnosis.io"
    const val TEST_BRIDGE = "https://bridge.bitea.one"

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    private var session: Session? = null
    private var storage: WCSessionStore? = null
    private var peerMeta: PeerMeta? = null

    private val callId = AtomicInteger(0)

    fun createCallId() = callId.incrementAndGet().toLong()

    fun isSupportWallet(context: Context, packageName: String): Boolean {
        val queryIntent = Intent(Intent.ACTION_MAIN)
        queryIntent.setPackage(packageName)
        val resolveInfo = context.packageManager.resolveActivity(queryIntent, 0)
        return resolveInfo != null
    }

    fun setCustomPeerMeta(peer: PeerMeta) {
        peerMeta = peer
    }

    fun setCustomSessionStore(context: Context) {
        storage = WCFileSessionStore(
            File(context.cacheDir, "session_store.json")
                .apply { createNewFile() }
        )
    }

    fun connect(
        context: Context,
        config: WCConfig,
        specialApp: String = "",
        callback: Session.Callback
    ) {
        // check parameters
        if (peerMeta == null) {
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
            config,
            WCPayloadAdapter(),
            storage!!,
            OkHttpTransport.Builder(client),
            peerMeta!!
        )
        session!!.offer()
    }

    fun approvedResult(): JSONObject? {
        return session?.approvedResult()
    }

    fun sendMessage(call: MethodCall) {
        session?.performMethodCall(call = call)
    }
}