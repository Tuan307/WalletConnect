package org.walletconnect

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object WCMoshi {
	val moshi: Moshi = Moshi.Builder()
		.add(KotlinJsonAdapterFactory())
		.build()
}