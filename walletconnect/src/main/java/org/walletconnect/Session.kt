package org.walletconnect

import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.WCStatus

interface Session {

	/**
	 * Send client info to the bridge and wait for a client to connect
	 */
	fun kill()

	fun addCallback(cb: Callback)
	fun removeCallback(cb: Callback)
	fun clearCallbacks()

	interface Callback {
		fun onStatus(status: WCStatus)
		fun onMethodCall(call: MethodCall)
	}

	interface PayloadAdapter {
		fun decrypt(payload: String, key: String): String
		fun encrypt(data: String, key: String): String
	}

	interface Transport {

		fun connect(): Boolean

		fun isConnected(): Boolean

		fun send(payload: String)

		fun close()
	}
}