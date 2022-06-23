package org.walletconnect.samples

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View

// throttle 350
fun View.throttleClickListener(onSafeClick: (View) -> Unit) {
	val safeClickListener = ThrottleClickListener {
		onSafeClick(it)
	}
	setOnClickListener(safeClickListener)
}

fun View.sendFakeClick(x: Float, y: Float) {
	val uptime = SystemClock.uptimeMillis()
	val event = MotionEvent.obtain(uptime, uptime, MotionEvent.ACTION_DOWN, x, y, 0)
	dispatchTouchEvent(event)
	event.action = MotionEvent.ACTION_UP
	dispatchTouchEvent(event)
}

class ThrottleClickListener(
	private var defaultInterval: Int = 350,
	private val onQuicklyClick: (View) -> Unit
) : View.OnClickListener {

	private var lastTimeClicked: Long = 0
	override fun onClick(v: View) {
		if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
			return
		}
		lastTimeClicked = SystemClock.elapsedRealtime()
		try {
			onQuicklyClick(v)
		} catch (throwable: Throwable) {
			throwable.printStackTrace()
		}
	}
}