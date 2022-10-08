package org.walletconnect.samples

import android.content.Context
import org.walletconnect.CallbackAdapter
import org.walletconnect.WalletConnect
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.PeerMeta
import org.walletconnect.entity.WCConfig
import org.walletconnect.tools.walletRandomKey
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.MessageDigest
import java.util.*


fun MainActivity.wcSetup() {

	val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("192.168.9.90", 8888))
	//val topic = appInstanceId
	val topic = UUID.randomUUID().toString()
	val peerId = UUID.randomUUID().toString()
	val peerApp = PeerData(
		peerId = peerId,
		peerMeta = PeerMeta(
			url = "https://example.com",
			name = getString(R.string.app_name),
			description = "WalletConnect Sample App",
		),
		chainId = 1,
	)

	WalletConnect.setCustomPeerMeta(
		PeerMeta(
			url = "https://example.com",
			name = getString(R.string.app_name),
			description = "WalletConnect Sample App",
		)
	)

	WalletConnect.setCustomSessionStore(applicationContext)

	val config = WCConfig(
		handshakeTopic = topic,
		bridge = WalletConnect.GNOSIS_BRIDGE,
		key = walletRandomKey(),
		protocol = "wc",
		version = 1
	)

	WalletConnect.connect(
		context = applicationContext,
		config = config,
		specialApp = "io.metamask",
		callback = CallbackAdapter {}
	)
}

private fun instanceId(context: Context): String {
	val appSHA =
		MessageDigest.getInstance("SHA-256")
			.digest(context.applicationContext.packageName.toByteArray())
	return Base64.getEncoder().encodeToString(appSHA)
}