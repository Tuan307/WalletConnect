package org.walletconnect.entity

data class WCError(val code: Long, val message: String)

data class TransportError(override val cause: Throwable) :
    RuntimeException("Transport exception caused by $cause", cause)

sealed class MethodCallException(val id: Long, val code: Long, message: String) :
    IllegalArgumentException(message) {
    // TODO define proper error codes
    class InvalidRequest(id: Long, request: String) :
        MethodCallException(id, 23, "Invalid request: $request")

    class InvalidAccount(id: Long, account: String) :
        MethodCallException(id, 3141, "Invalid account request: $account")
}
