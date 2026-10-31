package com.focusforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Background = Color(0xFF090B10)
private val Surface = Color(0xFF141923)
private val Accent = Color(0xFF9B8CFF)
private val Muted = Color(0xFF929AAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FocusForgeApp() }
    }
}

@Composable
fun FocusForgeApp() {
    var selectedTab by remember { mutableStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Background, surface = Surface, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Background, bottomBar = {
            NavigationBar(containerColor = Surface, contentColor = Muted) {
                val items = listOf("Home", "Planner", "Rooms", "Stats")
                val icons = listOf(Icons.Rounded.Home, Icons.Rounded.CalendarMonth, Icons.Rounded.Groups, Icons.Rounded.Analytics)
                items.forEachIndexed { index, label ->
                    NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index }, icon = { Icon(icons[index], contentDescription = label) }, label = { Text(label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Accent, selectedTextColor = Accent, indicatorColor = Accent.copy(alpha = .13f), unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding))
                1 -> PlaceholderScreen("Study planner", "Plan study blocks and daily targets.", Modifier.padding(padding))
                2 -> PlaceholderScreen("Study rooms", "Multiplayer rooms will connect through Supabase Realtime.", Modifier.padding(padding))
                else -> PlaceholderScreen("Your progress", "Focus history and weekly insights will appear here.", Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    var durationMinutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var completedSessions by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
            if (remainingSeconds == 0) { running = false; completedSessions += 1 }
        }
    }
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("FOCUSFORGE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Make time count.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(Surface), contentAlignment = Alignment.Center) { Text("FF", color = Accent, fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("TODAY'S FOCUS", "${completedSessions * durationMinutes} min", Modifier.weight(1f))
            StatCard("SESSIONS", completedSessions.toString(), Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Surface).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DEEP WORK", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.size(224.dp).clip(CircleShape).background(Background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%02d:%02d".format(minutes, seconds), fontSize = 48.sp, fontWeight = FontWeight.Light, color = Color.White)
                    Text(if (running) "STAY IN THE ZONE" else "READY WHEN YOU ARE", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(25, 50, 90).forEach { value ->
                    FilterChip(selected = durationMinutes == value, onClick = { if (!running) { durationMinutes = value; remainingSeconds = value * 60 } }, label = { Text("${value}m") })
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { running = !running }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10111A)), shape = RoundedCornerShape(16.dp)) {
                    Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text(if (running) "Pause" else "Start focus", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { running = false; remainingSeconds = durationMinutes * 60 }, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Refresh, contentDescription = "Reset") }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("YOUR INTENTION", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("One session at a time. Keep distractions outside.", color = Color.White, fontSize = 15.sp)
            Text("FocusForge is an independent project. App blocking and cloud rooms are being built in upcoming milestones.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Muted, fontSize = 14.sp)
    }
}
