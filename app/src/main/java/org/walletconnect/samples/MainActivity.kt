package org.walletconnect.samples

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.walletconnect.WalletConnect
import org.walletconnect.entity.WCError
import org.walletconnect.entity.WCStatus

class MainActivity : AppCompatActivity() {

    companion object {
        val accounts = mutableListOf<String>()
    }

    private var walletConnect: WalletConnect? = null
    private var accountAddress: String = ""
    private var accountSignature: String = ""
    private val wcError: (WCError) -> Unit = { wcError ->
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, wcError.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.wcSetup).throttleClickListener {
            walletConnect = wcSetup(callback = object : CallbackAdapter() {
                override fun onStatusChanged(status: WCStatus) {
                    if (status == WCStatus.Approved) {
                        //{"approved":true,"chainId":1,"networkId":0,"accounts":["0x7abc2f6c603adc67cdff5d20b90d15365dc7bf2f"],"rpcUrl":"","peerId":"04bd5c2d-915a-4682-8ffa-49372812a64c","peerMeta":{"description":"MetaMask Mobile app","url":"https:\/\/metamask.io","icons":["https:\/\/raw.githubusercontent.com\/MetaMask\/brand-resources\/master\/SVG\/metamask-fox.svg"],"name":"MetaMask","ssl":true}}
                        val result = walletConnect?.approvedResult()
                        Log.d(WalletConnect.TAG, result.toString())
                        result?.let {
                            val a = it.optJSONArray("accounts") ?: JSONArray()
                            accountAddress = a[0].toString()
                            Log.d("MainActivity", accountAddress.toString())
                            var size = a.length()
                            synchronized(accounts) {
                                accounts.clear()
                                while (size > 0) {
                                    size--
                                    accounts.add(a.getString(size))
                                }
                            }
                        }
                    }
                }
            },"com.wallet.crypto.trustapp")
            walletConnect?.openWalletApp(applicationContext,"com.wallet.crypto.trustapp")
        }

        findViewById<View>(R.id.personalSign).throttleClickListener {
            if (checkWalletConnect()) {
                val address = accounts.firstOrNull()
                if (address.isNullOrEmpty()) {
                    Toast.makeText(this, "accounts is null", Toast.LENGTH_SHORT).show()
                    return@throttleClickListener
                }
                walletConnect?.personalSign(address = address, message = "Hello World!") {
                    accountSignature = it.result.toString()
                    Log.d("MainActivity", it.result.toString())
                }
                walletConnect?.openWalletApp(applicationContext,"com.wallet.crypto.trustapp")
            }
        }

        findViewById<View>(R.id.trans).throttleClickListener {
            if (checkWalletConnect()) {
                sendTransaction()
            }
        }

        findViewById<View>(R.id.sessionRelease).throttleClickListener {
            if (checkWalletConnect()) {
                walletConnect?.release()
            }
        }
    }

    private fun checkWalletConnect(): Boolean {
        return walletConnect != null
    }

    override fun onDestroy() {
        walletConnect?.release()
        super.onDestroy()
    }
}