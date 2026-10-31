package com.focusforge.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch

@Composable
fun LoginRequiredScreen(isConfigured: Boolean) {
    val client = FocusForgeSupabase.clientOrNull()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF9B8CFF),
        background = Color(0xFF090B10),
        surface = Color(0xFF141923),
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "FOCUSFORGE",
                color = Color(0xFF9B8CFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Build your focus.",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to continue. Your focus tools and study history are available after authentication.",
                color = Color(0xFF929AAA),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isConfigured || client == null) {
                        Text(
                            "Authentication is not configured for this build. Add the FocusForge Supabase URL and publishable key, then rebuild.",
                            color = Color(0xFFFFB4AB)
                        )
                    } else {
                        Text("Your account is required to use FocusForge.", color = Color.White)
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    error = null
                                    try {
                                        client.auth.signInWith(Google)
                                    } catch (e: Exception) {
                                        error = e.message ?: "Google sign-in could not be started."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (busy) {
                                CircularProgressIndicator()
                            } else {
                                Text("Continue with Google")
                            }
                        }
                        error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthLoadingScreen() {
    MaterialTheme(colorScheme = darkColorScheme(
        background = Color(0xFF090B10),
        onBackground = Color.White
    )) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF9B8CFF))
            Spacer(Modifier.height(16.dp))
            Text("Checking your account…", color = Color.White)
        }
    }
}
