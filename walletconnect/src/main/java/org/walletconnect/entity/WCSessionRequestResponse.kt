package org.walletconnect.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.walletconnect.WalletConnect

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

@JsonClass(generateAdapter = true)
data class WCSessionRequestResult(
	@Json(name = "peerId")
	val peerId: String,
	@Json(name = "peerMeta")
	val peerMeta: ClientMeta,
	@Json(name = "approved")
	val approved: Boolean,
	@Json(name = "chainId")
	val chainId: Long,
	@Json(name = "accounts")
	val accounts: List<String>,
	@Json(name = "networkId")
	val networkId: Long,
)