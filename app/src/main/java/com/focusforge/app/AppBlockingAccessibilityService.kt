package com.focusforge.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AppBlockingAccessibilityService : AccessibilityService() {
 private var blocker: LinearLayout? = null
 private var manager: WindowManager? = null
 private var currentBlockedPackage: String? = null

 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  if (!FocusForgeSettings.isFocusRunning(this)) { removeBlocker(); return }
  val foreground = event?.packageName?.toString() ?: return
  if (foreground == packageName) { removeBlocker(); return }
  if (foreground in FocusForgeSettings.blockedApps(this)) showBlocker(foreground) else removeBlocker()
 }

 private fun showBlocker(pkg: String) {
  if (blocker != null && currentBlockedPackage == pkg) return
  removeBlocker()
  val panel = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(48,48,48,48); setBackgroundColor(Color.rgb(12,15,24))
   addView(TextView(this@AppBlockingAccessibilityService).apply { text="FOCUSFORGE"; textSize=15f; gravity=Gravity.CENTER; setTextColor(Color.rgb(170,151,255)) })
   addView(TextView(this@AppBlockingAccessibilityService).apply { text="This app is blocked during your focus session."; textSize=21f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(0,28,0,28) })
   addView(Button(this@AppBlockingAccessibilityService).apply { text="Return to home"; setOnClickListener { performGlobalAction(GLOBAL_ACTION_HOME); removeBlocker() } })
   addView(TextView(this@AppBlockingAccessibilityService).apply { text="Pause or end your focus session to use this app."; textSize=12f; gravity=Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0,24,0,0) })
  }
  val wm = getSystemService(WINDOW_SERVICE) as WindowManager
  val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT)
  try { wm.addView(panel, params); blocker=panel; manager=wm; currentBlockedPackage=pkg } catch (_: Exception) { blocker=null; manager=null; currentBlockedPackage=null }
 }
 private fun removeBlocker() {
  val old=blocker ?: return
  try { manager?.removeView(old) } catch (_: Exception) { }
  blocker=null; manager=null; currentBlockedPackage=null
 }
 override fun onInterrupt() { removeBlocker() }
 override fun onDestroy() { removeBlocker(); super.onDestroy() }
}