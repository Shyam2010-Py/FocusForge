package com.focusforge.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "focus_sessions")
data class FocusSession(@PrimaryKey(autoGenerate = true) val id: Long = 0, val startedAt: Long, val completedAt: Long, val durationSeconds: Int)

@Dao
interface FocusSessionDao {
 @Insert suspend fun insert(session: FocusSession)
 @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC") fun observeAll(): Flow<List<FocusSession>>
 @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM focus_sessions WHERE completedAt >= :start") fun observeFocusSecondsSince(start: Long): Flow<Int>
}

@Database(entities = [FocusSession::class], version = 1, exportSchema = false)
abstract class FocusDatabase : RoomDatabase() {
 abstract fun sessions(): FocusSessionDao
 companion object {
  @Volatile private var instance: FocusDatabase? = null
  fun get(context: Context): FocusDatabase = instance ?: synchronized(this) {
   instance ?: Room.databaseBuilder(context.applicationContext, FocusDatabase::class.java, "focusforge.db").build().also { instance = it }
  }
 }
}

enum class AppRule { DEFAULT, STUDY, BLOCKED, ALWAYS_ALLOWED }

object FocusForgeSettings {
 private const val FILE = "focusforge_settings"
 private const val STUDY = "study_apps"
 private const val BLOCKED = "blocked_apps"
 private const val ALWAYS = "always_allowed_apps"
 fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
 fun rule(context: Context, pkg: String): AppRule {
  val p = prefs(context)
  return when {
   p.getStringSet(ALWAYS, emptySet())?.contains(pkg) == true -> AppRule.ALWAYS_ALLOWED
   p.getStringSet(BLOCKED, emptySet())?.contains(pkg) == true -> AppRule.BLOCKED
   p.getStringSet(STUDY, emptySet())?.contains(pkg) == true -> AppRule.STUDY
   else -> AppRule.DEFAULT
  }
 }
 fun setRule(context: Context, pkg: String, rule: AppRule) {
  val p = prefs(context)
  val study = p.getStringSet(STUDY, emptySet())?.toMutableSet() ?: mutableSetOf()
  val blocked = p.getStringSet(BLOCKED, emptySet())?.toMutableSet() ?: mutableSetOf()
  val always = p.getStringSet(ALWAYS, emptySet())?.toMutableSet() ?: mutableSetOf()
  study.remove(pkg); blocked.remove(pkg); always.remove(pkg)
  when (rule) {
   AppRule.STUDY -> study.add(pkg)
   AppRule.BLOCKED -> blocked.add(pkg)
   AppRule.ALWAYS_ALLOWED -> always.add(pkg)
   AppRule.DEFAULT -> Unit
  }
  p.edit().putStringSet(STUDY, study).putStringSet(BLOCKED, blocked).putStringSet(ALWAYS, always).apply()
 }
 fun isFocusRunning(context: Context) = prefs(context).getBoolean("running", false)
 fun blockedApps(context: Context) = prefs(context).getStringSet(BLOCKED, emptySet())?.toSet() ?: emptySet()
}