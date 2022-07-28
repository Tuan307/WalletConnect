package org.walletconnect.security

import android.content.Context
import android.os.Build
import java.io.File

object SecurityStoreCompat {

	private val security: SecurityStore by lazy {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			SecurityStoreV23()
		} else {
			SecurityStoreV21()
		}
	}

	fun encrypt(context: Context, fileName: String, content: String) {
		security.deleteFile(
			context = context,
			fileName = fileName
		)
		security.encrypt(
			context = context,
			fileName = fileName,
			content = content
		)
	}

	fun decrypt(context: Context, fileName: String): String {
		return security.decrypt(context = context, fileName = fileName)
	}

	internal fun checkDirectory(file: File): File {
		if (!file.isDirectory) {
			file.delete()
		}
		if (!file.exists()) {
			file.mkdirs()
		}
		return file
	}
}