package org.walletconnect.samples

import android.app.Activity
import android.content.Context
import org.walletconnect.Session
import org.walletconnect.WalletConnect
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.PeerMeta
import org.walletconnect.entity.WCConfig
import org.walletconnect.tools.walletRandomKey
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.MessageDigest
import java.util.*


fun MainActivity.wcSetup(callback: Session.Callback, specialApp: String): WalletConnect {

    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("192.168.9.90", 8888))
    //val topic = appInstanceId
    val topic = UUID.randomUUID().toString()
    val peerId = instanceId(applicationContext)

    val peerApp = PeerData(
        peerId = peerId,
        peerMeta = PeerMeta(
            url = "https://gecko.game",
            name = getString(R.string.app_name),
            description = "WalletConnect Sample App",
        ),
        chainId = 1,
    )

    val config = WCConfig(
        handshakeTopic = topic,
        bridge = WalletConnect.GNOSIS_BRIDGE,
        key = walletRandomKey(),
        protocol = "wc",
        version = 1
    )

    return WalletConnect.connect(
        context = applicationContext,
        config = config,
        peerApp = peerApp,
        specialApp = specialApp,
        callback = callback
    )
}

private fun instanceId(context: Context): String {
    val appSHA =
        MessageDigest.getInstance("SHA-256")
            .digest(context.applicationContext.packageName.toByteArray())
    return Base64.getEncoder().encodeToString(appSHA)
}