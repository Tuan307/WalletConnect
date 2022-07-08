package org.walletconnect.tools

import java.math.BigInteger
import java.util.*

// Map functions that throw exceptions into optional types
internal fun <T> nullOnThrow(func: () -> T): T? = try {
	func.invoke()
} catch (e: Exception) {
	e.printStackTrace()
	null
}

internal fun tryExec(block: () -> Unit, error: (Exception) -> Unit) {
	try {
		block()
	} catch (e: Exception) {
		error.invoke(e)
	}
}

fun walletRandomKey(): String {
	return ByteArray(32).also { Random().nextBytes(it) }.toNoPrefixHexString()
}

fun BigInteger.biggerThan(value: Int): Boolean {
	return biggerThan(value.toLong())
}

fun BigInteger.biggerThan(value: Long): Boolean {
	val v = BigInteger.valueOf(value)
	return this > v
}

fun String?.decode2BigInteger(): BigInteger {
	return if (this == null || this.isEmpty()) {
		BigInteger.ZERO
	} else {
		this.removePrefix("0x").toBigInteger(16)
	}
}

fun BigInteger.encode2Hex(): String {
	val v = this.toString(16)
	return "0x$v"
}