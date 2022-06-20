package org.walletconnect

import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.PeerMeta
import org.walletconnect.entity.WCMessage
import org.walletconnect.entity.WCStatus

interface Session {

    fun init()

    /**
     * Send client info to the bridge and wait for a client to connect
     */
    fun offer()
    fun approve(accounts: List<String>, chainId: Long)
    fun reject()
    fun update(accounts: List<String>, chainId: Long)
    fun kill()

    fun peerMeta(): PeerMeta?
    fun approvedAccounts(): List<String>?

    fun approveRequest(id: Long, response: Any)
    fun rejectRequest(id: Long, errorCode: Long, errorMsg: String)
    fun performMethodCall(call: MethodCall, callback: ((MethodCall.Response) -> Unit)? = null)

    fun addCallback(cb: Callback)
    fun removeCallback(cb: Callback)
    fun clearCallbacks()

    interface Callback {
        fun onStatus(status: WCStatus)
        fun onMethodCall(call: MethodCall)
    }

    interface PayloadAdapter {
        fun parse(payload: String, key: String): MethodCall
        fun prepare(data: MethodCall, key: String): String
    }

    interface Transport {

        fun connect(): Boolean

        fun isConnected(): Boolean

        fun send(message: WCMessage)

        fun close()

        interface Builder {
            fun build(
                url: String,
                statusHandler: (WCStatus) -> Unit,
                messageHandler: (WCMessage) -> Unit
            ): Transport
        }
    }
}
