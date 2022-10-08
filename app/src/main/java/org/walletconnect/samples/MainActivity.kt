package org.walletconnect.samples

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.walletconnect.entity.WCError

class MainActivity : AppCompatActivity() {

	companion object {
		val accounts = mutableListOf<String>()
	}

	private val wcError: (WCError) -> Unit = { wcError ->
		lifecycleScope.launch {
			Toast.makeText(this@MainActivity, wcError.message, Toast.LENGTH_SHORT)
				.show()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		findViewById<View>(R.id.wcSetup).throttleClickListener {
			wcSetup()
		}

		findViewById<View>(R.id.personalSign).throttleClickListener {
			if (checkWalletConnect()) {
				val address = accounts.firstOrNull()
				if (address.isNullOrEmpty()) {
					Toast.makeText(this, "accounts is null", Toast.LENGTH_SHORT).show()
					return@throttleClickListener
				}
				//personalSign(message = "Hello World!")
				//walletConnect?.personalSign(
				//	message = "Hello World!",
				//	address = address,
				//	signResult = { sign ->
				//		lifecycleScope.launch {
				//			Toast.makeText(
				//				this@MainActivity,
				//				sign,
				//				Toast.LENGTH_SHORT
				//			).show()
				//		}
				//	},
				//	error = wcError
				//)
			}
		}

		findViewById<View>(R.id.trans).throttleClickListener {
			if (checkWalletConnect()) {
				sendTransaction()
			}
		}

		findViewById<View>(R.id.sessionRelease).throttleClickListener {
			if (checkWalletConnect()) {
				wcRelease()
			}
		}
	}

	private fun checkWalletConnect(): Boolean {
		return true
	}

	override fun onDestroy() {
		wcRelease()
		super.onDestroy()
	}
}