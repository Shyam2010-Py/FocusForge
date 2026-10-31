package com.focusforge.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val bg=Color(0xFF090B10); private val panel=Color(0xFF141923); private val accent=Color(0xFF9B8CFF); private val muted=Color(0xFF929AAA)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { FocusForgeApp() } }
}

@Composable fun FocusForgeApp() {
 var tab by remember { mutableIntStateOf(0) }
 MaterialTheme(colorScheme=darkColorScheme(primary=accent, background=bg, surface=panel, onBackground=Color.White, onSurface=Color.White)) {
  Scaffold(containerColor=bg, bottomBar={
   NavigationBar(containerColor=panel) {
    listOf("Focus","App rules","History").forEachIndexed { i,label ->
     val icon=listOf(Icons.Rounded.Home,Icons.Rounded.Block,Icons.Rounded.Analytics)[i]
     NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icon,contentDescription=label)},label={Text(label)})
    }
   }
  }) { padding ->
   when(tab) { 0->FocusScreen(Modifier.padding(padding)); 1->AppRulesScreen(Modifier.padding(padding)); else->HistoryScreen(Modifier.padding(padding)) }
  }
 }
}

@Composable private fun FocusScreen(modifier: Modifier=Modifier) {
 val context= LocalContext.current; val prefs=remember { FocusForgeSettings.prefs(context) }
 var selected by remember { mutableIntStateOf(25) }
 var remaining by remember { mutableLongStateOf(prefs.getLong("remaining_ms",1500000L)) }
 var running by remember { mutableStateOf(prefs.getBoolean("running",false)) }
 var hasSession by remember { mutableStateOf(prefs.contains("started_at")) }
 LaunchedEffect(Unit) { while(true) {
  running=prefs.getBoolean("running",false); hasSession=prefs.contains("started_at")
  remaining=if(running) (prefs.getLong("end_at",0L)-System.currentTimeMillis()).coerceAtLeast(0L) else prefs.getLong("remaining_ms",remaining)
  delay(500)
 } }
 Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  Text("FOCUSFORGE",color=accent,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
  Text("Make time count.",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)
  Card(colors=CardDefaults.cardColors(containerColor=panel)) {
   Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
    Text("FOCUS SESSION",color=muted,fontSize=12.sp,letterSpacing=2.sp); Spacer(Modifier.height(20.dp))
    Box(Modifier.size(220.dp).clip(CircleShape).background(bg),contentAlignment=Alignment.Center) {
     Column(horizontalAlignment=Alignment.CenterHorizontally) {
      Text("%02d:%02d".format(remaining/60000,(remaining/1000)%60),color=Color.White,fontSize=46.sp,fontWeight=FontWeight.Light)
      Text(if(running) "FOCUS ACTIVE" else "READY",color=muted,fontSize=11.sp)
     }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     listOf(25,50,90).forEach { value ->
      FilterChip(selected=selected==value,onClick={if(!running && !hasSession){selected=value;remaining=value*60000L;prefs.edit().putLong("remaining_ms",remaining).apply()}},label={Text("${value}m")})
     }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
     Button(onClick={if(running) FocusTimerService.pause(context) else if(hasSession && remaining>0) FocusTimerService.resume(context) else FocusTimerService.start(context,selected*60)}) {
      Icon(if(running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,contentDescription=null); Spacer(Modifier.width(5.dp))
      Text(if(running) "Pause" else if(hasSession && remaining>0) "Resume" else "Start focus")
     }
     OutlinedButton(onClick={FocusTimerService.reset(context);remaining=selected*60000L;running=false;hasSession=false}) { Icon(Icons.Rounded.Refresh,contentDescription="Reset") }
    }
   }
  }
  Text("Only apps marked Block are restricted during an active focus session.",color=muted,fontSize=13.sp)
  PermissionCard()
 }
}

@Composable private fun PermissionCard() {
 val context=LocalContext.current
 var notifications by remember { mutableStateOf(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) }
 var accessibility by remember { mutableStateOf(isAccessibilityEnabled(context)) }
 LaunchedEffect(Unit) { while(true) {
  notifications=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
  accessibility=isAccessibilityEnabled(context); delay(1000)
 } }
 Card(colors=CardDefaults.cardColors(containerColor=panel)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Permissions",color=Color.White,fontWeight=FontWeight.Bold)
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Text("Notifications",color=Color.White); Text(if(notifications) "Enabled" else "Needed for timer alerts",color=muted,fontSize=12.sp) }
    if(!notifications && Build.VERSION.SDK_INT>=33) OutlinedButton(onClick={(context as? ComponentActivity)?.let { ActivityCompat.requestPermissions(it,arrayOf(Manifest.permission.POST_NOTIFICATIONS),301) }}) { Text("Allow") } else Text("✓",color=Color(0xFF70D6A1))
   }
   HorizontalDivider(color=bg)
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Text("App blocking service",color=Color.White); Text(if(accessibility) "Enabled" else "Enable in Android Settings",color=muted,fontSize=12.sp) }
    OutlinedButton(onClick={context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}) { Text(if(accessibility) "Settings" else "Enable") }
   }
   Text("Accessibility identifies the foreground app to enforce your own blocked-app list. No screen content is uploaded.",color=muted,fontSize=11.sp)
  }
 }
}

private data class InstalledApp(val pkg:String,val label:String)

@Composable private fun AppRulesScreen(modifier:Modifier=Modifier) {
 val context=LocalContext.current; var refresh by remember { mutableIntStateOf(0) }
 val apps=remember(refresh) {
  val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  context.packageManager.queryIntentActivities(intent,PackageManager.MATCH_ALL)
   .map { InstalledApp(it.activityInfo.packageName,it.loadLabel(context.packageManager).toString()) }
   .filter { it.pkg!=context.packageName }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
 }
 Column(modifier.fillMaxSize().padding(16.dp)) {
  Text("App rules",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)
  Text("Choose which apps help you study, which to block, and which must always remain available.",color=muted,fontSize=12.sp)
  Spacer(Modifier.height(8.dp))
  Text("Study = allowed  •  Block = restricted during focus  •  Always allow = exempt",color=accent,fontSize=11.sp)
  OutlinedButton(onClick={refresh++},modifier=Modifier.fillMaxWidth()) { Text("Refresh installed apps") }
  LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) {
   items(apps,key={it.pkg}) { app ->
    var rule by remember(app.pkg) { mutableStateOf(FocusForgeSettings.rule(context,app.pkg)) }
    Card(colors=CardDefaults.cardColors(containerColor=panel)) {
     Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp)) {
      Text(app.label,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
      Text(app.pkg,color=muted,fontSize=10.sp)
      Column {
       listOf(AppRule.DEFAULT to "Default",AppRule.STUDY to "Study",AppRule.BLOCKED to "Block",AppRule.ALWAYS_ALLOWED to "Always allow").chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth()) {
         row.forEach { (value,label) ->
          Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically) {
           RadioButton(selected=rule==value,onClick={rule=value;FocusForgeSettings.setRule(context,app.pkg,value)})
           Text(label,color=if(rule==value) accent else muted,fontSize=11.sp)
          }
         }
        }
       }
      }
     }
    }
   }
  }
 }
}

private fun isAccessibilityEnabled(context:Context):Boolean {
 val target="${context.packageName}/${AppBlockingAccessibilityService::class.java.name}"
 val raw=Settings.Secure.getString(context.contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?:return false
 val splitter=TextUtils.SimpleStringSplitter(':'); splitter.setString(raw)
 return splitter.any { it.equals(target,ignoreCase=true) }
}

@Composable private fun HistoryScreen(modifier:Modifier=Modifier) {
 val context=LocalContext.current; val dao=remember { FocusDatabase.get(context).sessions() }
 val sessions by dao.observeAll().collectAsState(initial=emptyList())
 val start=remember { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }.timeInMillis }
 val today by dao.observeFocusSecondsSince(start).collectAsState(initial=0)
 Column(modifier.fillMaxSize().padding(18.dp)) {
  Text("Focus history",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)
  Text("Completed sessions saved on this device.",color=muted,fontSize=12.sp)
  Spacer(Modifier.height(14.dp))
  Card(colors=CardDefaults.cardColors(containerColor=panel)) {
   Column(Modifier.fillMaxWidth().padding(18.dp)) {
    Text("TODAY'S FOCUS",color=muted,fontSize=11.sp,fontWeight=FontWeight.Bold)
    Text("${today/60} minutes",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
    Text("${sessions.count { it.completedAt>=start }} completed sessions",color=muted,fontSize=12.sp)
   }
  }
  Spacer(Modifier.height(16.dp)); Text("Recent sessions",color=Color.White,fontWeight=FontWeight.SemiBold)
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   items(sessions,key={it.id}) { session ->
    Card(colors=CardDefaults.cardColors(containerColor=panel)) {
     Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween) {
      Text(DateFormat.getDateTimeInstance().format(Date(session.completedAt)),color=Color.White,fontSize=12.sp)
      Text("${session.durationSeconds/60} min",color=accent,fontWeight=FontWeight.Bold)
     }
    }
   }
  }
 }
}
