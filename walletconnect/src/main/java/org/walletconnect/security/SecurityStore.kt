package org.walletconnect.security

import android.content.Context
import java.nio.charset.Charset

interface SecurityStore {
	companion object {
		const val childDir = "wallet-connect"
		const val installId = "install_id"
		val UTF_8: Charset = Charset.forName("UTF-8")
	}

	fun encrypt(context: Context, fileName: String, content: String)
	fun decrypt(context: Context, fileName: String): String
	fun deleteFile(context: Context, fileName: String)
}