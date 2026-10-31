package com.focusforge.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FocusTimerService : Service() {
 private val handler = Handler(Looper.getMainLooper())
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
 private val ticker = object : Runnable {
  override fun run() {
   val p = FocusForgeSettings.prefs(this@FocusTimerService)
   if (!p.getBoolean("running", false)) return
   val ms = (p.getLong("end_at", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
   p.edit().putLong("remaining_ms", ms).apply()
   if (ms == 0L) complete() else { update(ms); handler.postDelayed(this, 1000L) }
  }
 }
 override fun onCreate() {
  super.onCreate()
  getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Focus sessions", NotificationManager.IMPORTANCE_LOW))
 }
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  val p = FocusForgeSettings.prefs(this)
  when (intent?.action) {
   START -> {
    val seconds = intent.getIntExtra(EXTRA_SECONDS, 1500).coerceIn(60, 14400)
    val now = System.currentTimeMillis()
    p.edit().putLong("started_at", now).putInt("duration_seconds", seconds).putLong("remaining_ms", seconds*1000L).putLong("end_at", now+seconds*1000L).putBoolean("running", true).apply()
    startForeground(ID, notification(seconds*1000L)); handler.removeCallbacks(ticker); handler.post(ticker)
   }
   PAUSE -> {
    val ms = (p.getLong("end_at", 0L)-System.currentTimeMillis()).coerceAtLeast(0L)
    p.edit().putLong("remaining_ms", ms).putBoolean("running", false).apply()
    handler.removeCallbacks(ticker); update(ms, "Focus paused")
   }
   RESUME -> {
    val ms = p.getLong("remaining_ms", 0L)
    if (ms > 0) {
     p.edit().putLong("end_at", System.currentTimeMillis()+ms).putBoolean("running", true).apply()
     startForeground(ID, notification(ms)); handler.removeCallbacks(ticker); handler.post(ticker)
    }
   }
   RESET -> {
    handler.removeCallbacks(ticker)
    p.edit().putBoolean("running", false).putLong("remaining_ms", 1500000L).remove("end_at").remove("started_at").remove("duration_seconds").apply()
    stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
   }
   null -> {
    if (p.getBoolean("running", false)) {
     val ms = (p.getLong("end_at", 0L)-System.currentTimeMillis()).coerceAtLeast(0L)
     if (ms > 0L) { startForeground(ID, notification(ms)); handler.post(ticker) } else complete()
    }
   }
  }
  return START_STICKY
 }
 private fun complete() {
  handler.removeCallbacks(ticker)
  val p = FocusForgeSettings.prefs(this)
  val start = p.getLong("started_at", System.currentTimeMillis())
  val duration = p.getInt("duration_seconds", 0)
  p.edit().putBoolean("running", false).putLong("remaining_ms", 0L).remove("end_at").apply()
  scope.launch {
   if (duration > 0) FocusDatabase.get(this@FocusTimerService).sessions().insert(FocusSession(startedAt=start, completedAt=System.currentTimeMillis(), durationSeconds=duration))
  }
  getSystemService(NotificationManager::class.java).notify(ID+1, NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Focus session complete").setContentText("Your session has been saved.").setAutoCancel(true).build())
  stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
 }
 private fun notification(ms: Long): Notification {
  val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("FocusForge session").setContentText("Remaining: %02d:%02d".format(ms/60000, (ms/1000)%60)).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).build()
 }
 private fun update(ms: Long, title: String = "FocusForge session") {
  getSystemService(NotificationManager::class.java).notify(ID, NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText("Remaining: %02d:%02d".format(ms/60000, (ms/1000)%60)).setOngoing(FocusForgeSettings.isFocusRunning(this)).setOnlyAlertOnce(true).build())
 }
 override fun onDestroy() { handler.removeCallbacks(ticker); scope.cancel(); super.onDestroy() }
 override fun onBind(intent: Intent?): IBinder? = null
 companion object {
  private const val CHANNEL="focusforge_focus"; private const val ID=701; private const val EXTRA_SECONDS="duration_seconds"
  const val START="focusforge.START"; const val PAUSE="focusforge.PAUSE"; const val RESUME="focusforge.RESUME"; const val RESET="focusforge.RESET"
  fun start(c: Context, seconds: Int)=c.startForegroundService(Intent(c, FocusTimerService::class.java).setAction(START).putExtra(EXTRA_SECONDS, seconds))
  fun pause(c: Context)=c.startService(Intent(c, FocusTimerService::class.java).setAction(PAUSE))
  fun resume(c: Context)=c.startForegroundService(Intent(c, FocusTimerService::class.java).setAction(RESUME))
  fun reset(c: Context)=c.startService(Intent(c, FocusTimerService::class.java).setAction(RESET))
 }
}