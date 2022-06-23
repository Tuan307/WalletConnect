package org.walletconnect

import org.walletconnect.entity.WCConfig
import java.net.URLDecoder
import java.net.URLEncoder

fun WCConfig.toWCUri() =
	"wc:$handshakeTopic@$version?bridge=${URLEncoder.encode(bridge, "UTF-8")}&key=$key"

fun WCConfig.fromWCUri(uri: String): WCConfig {

	val protocolSeparator = uri.indexOf(':')
	val handshakeTopicSeparator = uri.indexOf('@', startIndex = protocolSeparator)
	val versionSeparator = uri.indexOf('?')
	val protocol = uri.substring(0, protocolSeparator)
	val handshakeTopic = uri.substring(protocolSeparator + 1, handshakeTopicSeparator)

	if (versionSeparator < 1) {
		throw IllegalArgumentException("Invalid WC URI: $uri versionSeparator:$versionSeparator")
	}

	val version = Integer.valueOf(
		uri.substring(
			handshakeTopicSeparator + 1,
			versionSeparator
		)
	)

	val params = uri.substring(versionSeparator + 1).split("&").associate {
		it.split("=")
			.let { param -> param.first() to URLDecoder.decode(param[1], "UTF-8") }
	}

	val bridge = params["bridge"]
		?: throw IllegalArgumentException("Missing bridge param in URI")
	val key =
		params["key"] ?: throw IllegalArgumentException("Missing key param in URI")
	return WCConfig(handshakeTopic, bridge, key, protocol, version)
}