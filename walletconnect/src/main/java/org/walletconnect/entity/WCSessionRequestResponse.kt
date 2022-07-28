package org.walletconnect.entity

import org.json.JSONObject

/**
 * https://docs.walletconnect.com/tech-spec#session-request
 * <pre>
 * interface WCSessionRequestRequest {
 *      id: number;
 *      jsonrpc: "2.0";
 *      method: "wc_sessionRequest";
 *      params: [
 *          {
 *              peerId: string;
 *              peerMeta: ClientMeta;
 *              chainId?: number | null;
 *          }
 *      ];
 * }
 *
 * interface WCSessionRequestResponse {
 *      id: number;
 *      jsonrpc: "2.0";
 *      result: {
 *          peerId: string;
 *          peerMeta: ClientMeta;
 *          approved: boolean;
 *          chainId: number;
 * 		    accounts: string[];
 * 	    };
 * }
 *
 * </pre>
 */

data class WCSessionRequestRequest(
	val requestId: Long,
	val peerId: String,
	val peerMeta: ClientMeta,
	val chainId: Int? = null
) : WCSessionRequest(
	id = requestId,
	jsonrpc = "2.0",
	method = "wc_sessionRequest",
	params = listOf(
		JSONObject().apply {
			put("peerId", peerId)
			put("peerMeta", peerMeta.toJSON())
			chainId?.let { put("chainId", chainId) }
		}
	)
)

data class WCSessionRequestResult(
	val peerId: String,
	val peerMeta: ClientMeta,
	val approved: Boolean,
	val chainId: Long,
	val accounts: List<String>,
	val networkId: Long,
)

data class WCSessionRequestResponse(
	private val requestId: Long,
	private val wcSessionRequestResult: WCSessionRequestResult
) :
	WCSessionResponse<WCSessionRequestResult>(
		id = requestId,
		jsonrpc = "2.0",
		result = wcSessionRequestResult
	)