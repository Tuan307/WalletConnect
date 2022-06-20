# android-wallet-connect

A walletconnect implementation written by kotlin for Android platform. It is suggested to modify the
source code directly

## Useage

```kotlin
// topic means the session id,use for confirm the session between app and walletconnect
val topic = UUID.randomUUID().toString()
// PeerMeta is your app information, you can use any string
WalletConnect.setCustomPeerMeta(
	PeerMeta(
		url = "https://example.com",
		name = getString(R.string.app_name),
		description = "WalletConnect Sample App",
	)
)

// custom a session store, you can use any implementation, default use a file store.
WalletConnect.setCustomSessionStore(applicationContext)

// create a session config. handshakeTopic is the topic you want to use for handshake,
// bridge is the server address, the server used for communication between app and walletconnect
// you can use any server which you want, this is the third party server.
// const val WC_BRIDGE = "https://bridge.walletconnect.org"
// const val GNOSIS_BRIDGE = "https://safe-walletconnect.gnosis.io"
val config = WCConfig(
	handshakeTopic = topic,
	bridge = WalletConnect.GNOSIS_BRIDGE,
	key = walletRandomKey(),
	protocol = "wc",
	version = 1,
)

// create a connect
// specialApp means you want to use the app as the walletconnect client, default is null string
// specialApp is null, wc protocol will use all walletconnect client which support wc protocol
WalletConnect.connect(
	context = applicationContext,
	config = config,
	specialApp = "io.metamask",
	callback = CallbackAdapter(connectApproved = { list ->
		if (list.isNotEmpty()) {
			WalletConnect.release()
			val accounts = list.joinToString()
			Toast.makeText(this, "Connected to $accounts", Toast.LENGTH_SHORT).show()
		}
	})
)
```