package org.walletconnect.samples

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

	companion object {
		val accounts = mutableListOf<String>()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		wcSetup()


		findViewById<View>(R.id.sessionRequest).throttleClickListener {
			wcSessionRequest()
		}

		findViewById<View>(R.id.sessionUpdate).throttleClickListener {
			wcSessionUpdate()
		}
		findViewById<View>(R.id.ethSign).throttleClickListener {
			ethSign()
		}

		findViewById<View>(R.id.personalSign).throttleClickListener {
			personalSign()
		}

		findViewById<View>(R.id.trans).throttleClickListener {
			sendTransaction()
		}

		findViewById<View>(R.id.sessionRelease).throttleClickListener {
			wcRelease()
		}
	}

	override fun onDestroy() {
		wcRelease()
		super.onDestroy()
	}
}