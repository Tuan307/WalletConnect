package org.walletconnect.impls

import android.util.Log
import org.json.JSONObject
import org.walletconnect.Session
import org.walletconnect.WalletConnect
import org.walletconnect.entity.MethodCall
import org.walletconnect.entity.MethodCallException
import org.walletconnect.entity.PeerData
import org.walletconnect.entity.PeerMeta
import org.walletconnect.entity.SessionParams
import org.walletconnect.entity.TransportError
import org.walletconnect.entity.WCConfig
import org.walletconnect.entity.WCError
import org.walletconnect.entity.WCMessage
import org.walletconnect.entity.WCState
import org.walletconnect.entity.WCStatus
import org.walletconnect.tools.nullOnThrow
import java.util.Collections
import java.util.Random
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WCSession(
    private val config: WCConfig,
    private val payloadAdapter: Session.PayloadAdapter,
    private val sessionStore: WCSessionStore,
    transportBuilder: Session.Transport.Builder,
    clientMeta: PeerMeta,
    clientId: String? = null
) : Session {

    private val keyLock = Any()

    // Persisted state
    private var currentKey: String

    private var approvedAccounts: List<String>? = null
    private var chainId: Long? = null
    private var handshakeId: Long? = null
    private var peerId: String? = null
    private var peerMeta: PeerMeta? = null

    private val clientData: PeerData

    // Getters
    private val encryptionKey: String
        get() = currentKey

    private val decryptionKey: String
        get() = currentKey

    // Non-persisted state
    private val transport = transportBuilder.build(
        url = config.bridge,
        statusHandler = ::handleStatus,
        messageHandler = ::handleMessage
    )
    private val requests: MutableMap<Long, (MethodCall.Response) -> Unit> =
        ConcurrentHashMap()
    private val sessionCallbacks: MutableSet<Session.Callback> =
        Collections.newSetFromMap(ConcurrentHashMap<Session.Callback, Boolean>())

    init {
        currentKey = config.key
        clientData = sessionStore.load(config.handshakeTopic)?.let {
            currentKey = it.currentKey
            approvedAccounts = it.approvedAccounts
            chainId = it.chainId
            handshakeId = it.handshakeId
            peerId = it.peerData?.id
            peerMeta = it.peerData?.meta
            if (clientId != null && clientId != it.clientData.id)
                throw IllegalArgumentException("Provided clientId is different from stored clientId")
            it.clientData
        } ?: run {
            PeerData(clientId ?: UUID.randomUUID().toString(), clientMeta)
        }
        storeSession()
    }

    override fun addCallback(cb: Session.Callback) {
        sessionCallbacks.add(cb)
    }

    override fun removeCallback(cb: Session.Callback) {
        sessionCallbacks.remove(cb)
    }

    override fun clearCallbacks() {
        sessionCallbacks.clear()
    }

    private fun propagateToCallbacks(action: Session.Callback.() -> Unit) {
        sessionCallbacks.forEach {
            try {
                it.action()
            } catch (t: Throwable) {
                // If error propagation fails, don't try again
                nullOnThrow { it.onStatus(WCStatus.Error(t)) }
            }
        }
    }

    override fun peerMeta(): PeerMeta? = peerMeta

    override fun approvedAccounts(): List<String>? = approvedAccounts

    override fun init() {
        if (transport.connect()) {
            // Register for all messages for this client
            transport.send(
                WCMessage(
                    config.handshakeTopic, "sub", ""
                )
            )
        }
    }

    override fun offer() {
        if (transport.connect()) {
            val requestId = createCallId()
            send(
                MethodCall.SessionRequest(requestId, clientData),
                topic = config.handshakeTopic,
                callback = { resp ->
                    if (resp.result == null) {
                        Log.d(WalletConnect.TAG, "SessionRequest failed, resp.result is null.")
                        return@send
                    }
                    if (resp.result is JSONObject) {
                        val params = resp.result.extractSessionParams()
                        peerId = params.peerData?.id
                        peerMeta = params.peerData?.meta
                        approvedAccounts = params.accounts
                        chainId = params.chainId
                        storeSession()
                        propagateToCallbacks {
                            onStatus(
                                if (params.approved) {
                                    WCStatus.Approved
                                } else {
                                    WCStatus.Closed
                                }
                            )
                        }
                    } else {
                        Log.d(WalletConnect.TAG, "unknown result type.${resp.result}")
                    }
                }
            )
            handshakeId = requestId
        }
    }

    override fun approve(accounts: List<String>, chainId: Long) {
        val handshakeId = handshakeId ?: run {
            Log.d(WalletConnect.TAG, "approve: handshakeId is null")
            return
        }
        approvedAccounts = accounts
        this.chainId = chainId
        // We should not use classes in the Response, since this will not work with proguard
        val params = SessionParams(true, chainId, accounts, clientData)
        send(MethodCall.Response(handshakeId, params))
        storeSession()
        propagateToCallbacks { onStatus(WCStatus.Approved) }
    }

    override fun update(accounts: List<String>, chainId: Long) {
        val params = SessionParams(true, chainId, accounts, clientData)
        send(MethodCall.SessionUpdate(createCallId(), params))
    }

    override fun reject() {
        handshakeId?.let {
            // We should not use classes in the Response, since this will not work with proguard
            val params = SessionParams(false, null, null, null)
            send(MethodCall.Response(it, params))
        }
        endSession()
    }

    override fun approveRequest(id: Long, response: Any) {
        send(MethodCall.Response(id, response))
    }

    override fun rejectRequest(id: Long, errorCode: Long, errorMsg: String) {
        send(
            MethodCall.Response(
                id,
                result = null,
                error = WCError(errorCode, errorMsg)
            )
        )
    }

    override fun performMethodCall(
        call: MethodCall,
        callback: ((MethodCall.Response) -> Unit)?
    ) {
        send(call, callback = callback)
    }

    private fun handleStatus(status: WCStatus) {
        when (status) {
            WCStatus.Connected -> {
                // Register for all messages for this client
                transport.send(
                    WCMessage(
                        clientData.id, "sub", ""
                    )
                )
            }
            else -> {}
        }
        propagateToCallbacks {
            onStatus(
                when (status) {
                    WCStatus.Connected -> WCStatus.Connected
                    WCStatus.Disconnected -> WCStatus.Disconnected
                    is WCStatus.Error -> WCStatus.Error(
                        TransportError(
                            status.throwable
                        )
                    )
                    else -> {
                        Log.d(WalletConnect.TAG, "unknown status type.$status")
                        status
                    }
                }
            )
        }
    }

    private fun handleMessage(message: WCMessage) {
        if (message.type != "pub") {
            if (WalletConnect.DEBUG_LOG) {
                Log.d(
                    WalletConnect.TAG,
                    "handleMessage: unknown message type: ${message.type}"
                )
            }
            return
        }

        val data: MethodCall
        synchronized(keyLock) {
            try {
                data = payloadAdapter.parse(message.payload, decryptionKey)
            } catch (e: Exception) {
                e.printStackTrace()
                handlePayloadError(e)
                return
            }
        }

        var accountToCheck: String? = null
        when (data) {
            is MethodCall.SessionRequest -> {
                handshakeId = data.id
                peerId = data.peer.id
                peerMeta = data.peer.meta
                storeSession()
            }
            is MethodCall.SessionUpdate -> {
                if (!data.params.approved) {
                    endSession()
                }
                // TODO handle session update -> not important for our usecase
            }
            is MethodCall.SendTransaction -> {
                accountToCheck = data.from
            }
            is MethodCall.SignMessage -> {
                accountToCheck = data.address
            }
            is MethodCall.Response -> {
                val callback = requests[data.id] ?: return
                callback(data)
            }
            else -> {}
        }

        if (accountToCheck?.let
            { accountCheck(data.id(), it) } != false
        ) {
            propagateToCallbacks { onMethodCall(data) }
        }
    }

    private fun accountCheck(id: Long, address: String): Boolean {
        approvedAccounts?.find { it.equals(address, ignoreCase = true) } ?: run {
            handlePayloadError(MethodCallException.InvalidAccount(id, address))
            return false
        }
        return true
    }

    private fun handlePayloadError(e: Exception) {
        propagateToCallbacks { WCStatus.Error(e) }
        (e as? MethodCallException)?.let {
            rejectRequest(it.id, it.code, it.message ?: "Unknown error")
        }
    }

    private fun endSession() {
        sessionStore.remove(config.handshakeTopic)
        approvedAccounts = null
        chainId = null
        internalClose()
        propagateToCallbacks { onStatus(WCStatus.Closed) }
    }

    private fun storeSession() {
        sessionStore.store(
            config.handshakeTopic,
            WCState(
                config,
                clientData,
                peerId?.let { PeerData(it, peerMeta) },
                handshakeId,
                currentKey,
                approvedAccounts,
                chainId
            )
        )
    }

    // Returns true if method call was handed over to transport
    private fun send(
        msg: MethodCall,
        topic: String? = peerId,
        callback: ((MethodCall.Response) -> Unit)? = null
    ): Boolean {
        topic ?: return false

        val payload: String
        synchronized(keyLock) {
            payload = payloadAdapter.prepare(msg, encryptionKey)
        }
        callback?.let {
            requests[msg.id()] = callback
        }
        transport.send(WCMessage(topic, "pub", payload))
        return true
    }

    private fun createCallId() = System.currentTimeMillis() * 1000 + Random().nextInt(999)

    private fun internalClose() {
        transport.close()
    }

    override fun kill() {
        val params = SessionParams(false, null, null, null)
        send(MethodCall.SessionUpdate(createCallId(), params))
        endSession()
    }
}

interface WCSessionStore {
    fun load(id: String): WCState?

    fun store(id: String, state: WCState)

    fun remove(id: String)

    fun list(): List<WCState>
}
