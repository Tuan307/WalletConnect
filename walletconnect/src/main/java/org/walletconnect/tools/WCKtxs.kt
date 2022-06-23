package org.walletconnect.tools

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