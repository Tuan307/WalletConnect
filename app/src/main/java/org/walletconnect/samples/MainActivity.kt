package org.walletconnect.samples

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.walletconnect.CallbackAdapter
import org.walletconnect.WalletConnect
import org.walletconnect.entity.PeerMeta
import org.walletconnect.entity.WCConfig
import org.walletconnect.tools.walletRandomKey
import java.util.*

class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		findViewById<View>(R.id.metaMask).setOnClickListener {
			val topic = UUID.randomUUID().toString()
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
				version = 1,
			)

			WalletConnect.connect(
				context = applicationContext,
				config = config,
				specialApp = "io.metamask",
				callback = CallbackAdapter(connectApproved = { list ->
					if (list.isNotEmpty()) {
						WalletConnect.release()
						lifecycleScope.launch {
							val accounts = list.joinToString()
							Toast.makeText(
								this@MainActivity,
								"Connected to $accounts",
								Toast.LENGTH_SHORT
							)
								.show()
						}
					}
				})
			)
		}
	}
}