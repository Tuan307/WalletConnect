package org.walletconnect.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.ByteArrayOutputStream
import java.io.File

internal class SecurityStoreV23 : SecurityStore {

	override fun encrypt(context: Context, fileName: String, content: String) {
		val applicationContext = context.applicationContext
		val dir = File(applicationContext.filesDir, SecurityStore.childDir)
		SecurityStoreCompat.checkDirectory(dir)

		// Although you can define your own key generation parameter specification, it's
		// recommended that you use the value specified here.
		val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
		val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
		val file = File(dir, "$fileName-v23")
		// Create a file with this name, or replace an entire existing file
		// that has the same name. Note that you cannot append to an existing file,
		// and the file name cannot contain path separators.
		val encryptedFile = EncryptedFile.Builder(
			file,
			applicationContext,
			mainKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		val fileContent = content.toByteArray(SecurityStore.UTF_8)
		encryptedFile.openFileOutput().use { fos ->
			fos.write(fileContent)
			fos.flush()
		}
	}

	override fun decrypt(context: Context, fileName: String): String {
		val applicationContext = context.applicationContext
		val dir = File(applicationContext.filesDir, SecurityStore.childDir)
		SecurityStoreCompat.checkDirectory(dir)
		// Although you can define your own key generation parameter specification, it's
		// recommended that you use the value specified here.
		val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
		val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
		val file = File(dir, "$fileName-v23")
		if (!file.exists()) {
			return ""
		}
		val encryptedFile = EncryptedFile.Builder(
			file,
			applicationContext,
			mainKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		val inputStream = encryptedFile.openFileInput()
		val byteArrayOutputStream = ByteArrayOutputStream()
		inputStream.use {
			var nextByte: Int = inputStream.read()
			while (nextByte != -1) {
				byteArrayOutputStream.write(nextByte)
				nextByte = inputStream.read()
			}
		}
		val plaintext: ByteArray = byteArrayOutputStream.toByteArray()
		byteArrayOutputStream.close()
		return String(plaintext, SecurityStore.UTF_8)
	}

	override fun deleteFile(context: Context, fileName: String) {
		if (fileName.isBlank()) return
		val dir = File(context.applicationContext.filesDir, SecurityStore.childDir)
		val file = File(dir, "$fileName-v23")
		if (file.exists()) file.delete()
	}
}