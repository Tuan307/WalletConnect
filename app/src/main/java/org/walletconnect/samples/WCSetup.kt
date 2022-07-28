package org.walletconnect.samples

import org.walletconnect.WalletConnect
import org.walletconnect.entity.ClientMeta
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.WCConfig
import org.walletconnect.tools.walletRandomKey
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.MessageDigest
import java.util.*


fun MainActivity.wcSetup(): WalletConnect {


	val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("192.168.9.90", 8888))

	val appSHA =
		MessageDigest.getInstance("SHA-256").digest(applicationContext.packageName.toByteArray())
	val appInstanceId = Base64.getEncoder().encodeToString(appSHA)
	//val topic = appInstanceId
	val topic = UUID.randomUUID().toString()
	val peerId = UUID.randomUUID().toString()
	val peerApp = PeerData(
		peerId = peerId,
		peerMeta = ClientMeta(
			url = "https://example.com",
			name = getString(R.string.app_name),
			description = "WalletConnect Sample App",
		),
		chainId = null,
	)

	val config = WCConfig(
		context = applicationContext,
		topic = topic,
		bridge = WalletConnect.WC_BRIDGE,
		key = walletRandomKey(),
		protocol = "wc",
		version = 1,
		proxy = proxy,
	)

	return WalletConnect.connect(
		wcConfig = config,
		peerData = peerApp
	)
}