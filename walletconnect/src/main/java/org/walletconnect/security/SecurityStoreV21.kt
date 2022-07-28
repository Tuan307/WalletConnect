package org.walletconnect.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("DEPRECATION")
internal class SecurityStoreV21 : SecurityStore {

	companion object {
		private const val IV_LENGTH = 12
		private const val AAD_LENGTH = 16
		private const val TAG_LENGTH = 16
	}

	override fun encrypt(context: Context, fileName: String, content: String) {
		val applicationContext = context.applicationContext
		val dir = File(applicationContext.filesDir, SecurityStore.childDir)
		SecurityStoreCompat.checkDirectory(dir)

		val aesKey = generateAesKey(context)

		val contentArray = content.toByteArray(SecurityStore.UTF_8)
		val output = encrypt(aesKey, contentArray)

		val file = File(dir, "$fileName-v21")

		val size = output.iv.size + output.aad.size + output.tag.size + output.ciphertext.size
		val byteArray = ByteArray(size)
		var offset = 0

		for (i in 0 until output.iv.size) {
			byteArray[offset] = output.iv[i]
			offset += 1
		}
		for (i in 0 until output.aad.size) {
			byteArray[offset] = output.aad[i]
			offset += 1
		}
		for (i in 0 until output.tag.size) {
			byteArray[offset] = output.tag[i]
			offset += 1
		}
		for (i in 0 until output.ciphertext.size) {
			byteArray[offset] = output.ciphertext[i]
			offset += 1
		}
		file.writeBytes(byteArray)
	}

	override fun decrypt(context: Context, fileName: String): String {
		val applicationContext = context.applicationContext
		val dir = File(applicationContext.filesDir, SecurityStore.childDir)
		SecurityStoreCompat.checkDirectory(dir)

		val file = File(dir, "$fileName-v21")

		val byteArray = file.readBytes()

		val keySpec = generateAesKey(context)

		val ivEnd = IV_LENGTH
		val addEnd = IV_LENGTH + AAD_LENGTH
		val tagEnd = IV_LENGTH + AAD_LENGTH + TAG_LENGTH

		val iv = byteArray.copyOfRange(0, ivEnd)
		val add = byteArray.copyOfRange(ivEnd, addEnd)
		val tag = byteArray.copyOfRange(addEnd, tagEnd)
		val content = byteArray.copyOfRange(tagEnd, byteArray.size)

		val result = decrypt(keySpec, iv, add, tag, content)
		return String(result, SecurityStore.UTF_8)
	}

	override fun deleteFile(context: Context, fileName: String) {
		if (fileName.isBlank()) return
		val dir = File(context.applicationContext.filesDir, SecurityStore.childDir)
		val file = File(dir, "$fileName-v21")
		if (file.exists()) file.delete()
	}

	private class EncryptionOutput(
		val iv: ByteArray,
		val aad: ByteArray,
		val tag: ByteArray,
		val ciphertext: ByteArray
	)

	private fun encrypt(key: SecretKey, message: ByteArray): EncryptionOutput {
		val cipher = Cipher.getInstance("AES/GCM/NoPadding")
		cipher.init(Cipher.ENCRYPT_MODE, key)
		val iv = cipher.iv.copyOf()
		val aad = SecureRandom().generateSeed(AAD_LENGTH)
		cipher.updateAAD(aad)
		val result = cipher.doFinal(message)
		val ciphertext = result.copyOfRange(0, result.size - TAG_LENGTH)
		val tag = result.copyOfRange(result.size - TAG_LENGTH, result.size)
		return EncryptionOutput(iv = iv, aad = aad, tag = tag, ciphertext = ciphertext)
	}

	private fun decrypt(
		key: SecretKey,
		iv: ByteArray,
		aad: ByteArray,
		tag: ByteArray,
		ciphertext: ByteArray
	): ByteArray {
		val cipher = Cipher.getInstance("AES/GCM/NoPadding")
		val spec = GCMParameterSpec(TAG_LENGTH * 8, iv)
		cipher.init(Cipher.DECRYPT_MODE, key, spec)
		cipher.updateAAD(aad)
		return cipher.doFinal(ciphertext + tag)
	}

	@SuppressLint("PackageManagerGetSignatures")
	private fun generateAesKey(context: Context): SecretKey {
		val packageName = context.packageName
		val packageInfo =
			context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
		val signature = packageInfo.signatures[0]
		val signSeed = signature.toByteArray()

		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val installId = if (sharedPreferences.contains(SecurityStore.installId)) {
			var storeValue = sharedPreferences.getString(SecurityStore.installId, "")
			if (storeValue.isNullOrEmpty()) {
				storeValue = UUID.randomUUID().toString()
				sharedPreferences.edit().putString(SecurityStore.installId, storeValue).commit()
			}
			storeValue
		} else {
			val generatorUUID = UUID.randomUUID().toString()
			sharedPreferences.edit().putString(SecurityStore.installId, generatorUUID).commit()
			generatorUUID
		}

		val messageDigest: MessageDigest = MessageDigest.getInstance("SHA1")
		messageDigest.update(signSeed)
		messageDigest.update(installId.toByteArray(SecurityStore.UTF_8))
		val key = messageDigest.digest()
		return SecretKeySpec(key, 2, 16, "AES")
	}
}