package org.walletconnect.samples

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.walletconnect.WalletConnect

class MainActivity : AppCompatActivity() {

	companion object {
		val accounts = mutableListOf<String>()
	}

	private var walletConnect: WalletConnect? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		findViewById<View>(R.id.wcSetup).throttleClickListener {
			walletConnect = wcSetup()
		}

		findViewById<View>(R.id.sessionRequest).throttleClickListener {
			if (checkWalletConnect()) {
				walletConnect?.sessionRequest(result = { result ->
					accounts.clear()
					accounts.addAll(result.accounts)
					lifecycleScope.launch {
						Toast.makeText(
							this@MainActivity,
							result.accounts.joinToString(),
							Toast.LENGTH_SHORT
						)
							.show()
					}
				}, error = { wcError ->
					lifecycleScope.launch {

						Toast.makeText(this@MainActivity, wcError.message, Toast.LENGTH_SHORT)
							.show()
					}
				})
			}
		}

		findViewById<View>(R.id.sessionUpdate).throttleClickListener {
			if (checkWalletConnect()) {
				walletConnect?.wcSessionUpdate(
					approved = true,
					accounts = accounts
				)
			}
		}
		findViewById<View>(R.id.ethSign).throttleClickListener {
			if (checkWalletConnect()) {
				val address = accounts.firstOrNull()
				if (address.isNullOrEmpty()) {
					Toast.makeText(this, "accounts is null", Toast.LENGTH_SHORT).show()
					return@throttleClickListener
				}
				walletConnect?.ethSign(
					address = address,
					message = "Hello World!"
				)
			}
		}

		findViewById<View>(R.id.personalSign).throttleClickListener {
			if (checkWalletConnect()) {
				val address = accounts.firstOrNull()
				if (address.isNullOrEmpty()) {
					Toast.makeText(this, "accounts is null", Toast.LENGTH_SHORT).show()
					return@throttleClickListener
				}
				walletConnect?.personalSign(
					address = address,
					message = "Hello World!"
				)
			}
		}

		findViewById<View>(R.id.trans).throttleClickListener {
			if (checkWalletConnect()) {
				walletConnect?.let { wc -> sendTransaction(wc) }
			}
		}

		findViewById<View>(R.id.sessionRelease).throttleClickListener {
			if (checkWalletConnect()) {
				walletConnect?.let { wc -> wcRelease(wc) }
			}
		}
	}

	private fun checkWalletConnect(): Boolean {
		if (walletConnect == null) {
			Toast.makeText(this, "Setup wallet connect first", Toast.LENGTH_SHORT)
				.show()
			return false
		}
		return true
	}

	override fun onDestroy() {
		walletConnect?.let { wc -> wcRelease(wc) }
		super.onDestroy()
	}
}