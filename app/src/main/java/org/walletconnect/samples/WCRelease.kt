package org.walletconnect.samples

import org.walletconnect.WalletConnect

fun MainActivity.wcRelease(walletConnect: WalletConnect) {
	walletConnect.release()
}