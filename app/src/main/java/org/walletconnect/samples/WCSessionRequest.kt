package org.walletconnect.samples

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.walletconnect.CallbackAdapter
import org.walletconnect.WalletConnect
import org.walletconnect.entity.WCConfig
import org.walletconnect.tools.walletRandomKey
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*

/**
 * [wc_sessionRequest](https://docs.walletconnect.com/tech-spec#session-request)
 */
fun MainActivity.wcSessionRequest() {

	val context = this
	val topic = UUID.randomUUID().toString()

	val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("192.168.9.90", 8888))

	val config = WCConfig(
		topic = topic,
		bridge = WalletConnect.WC_BRIDGE,
		key = walletRandomKey(),
		protocol = "wc",
		version = 1,
		proxy = proxy,
	)

	WalletConnect.connect(
		context = applicationContext,
		config = config,
		callback = CallbackAdapter(connectApproved = { list ->
			if (list.isNotEmpty()) {
				// TODO after use, release the session
				// WalletConnect.release()
				lifecycleScope.launch {
					val accounts = list.joinToString()
					MainActivity.accounts.clear()
					MainActivity.accounts.addAll(list)

					Toast.makeText(
						context,
						"Connected to $accounts",
						Toast.LENGTH_SHORT
					)
						.show()
				}
			}
		})
	)
}