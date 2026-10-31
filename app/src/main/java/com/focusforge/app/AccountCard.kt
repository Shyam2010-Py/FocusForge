package com.focusforge.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google

@Composable
fun AccountCard() {
    val client = FocusForgeSupabase.clientOrNull()
    var email by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(client) {
        if (client != null) {
            while (true) {
                email = client.auth.currentUserOrNull()?.email
                delay(1500)
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141923))) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Account", color = Color.White)
            when {
                client == null -> {
                    Text(
                        "Cloud account is not configured on this build. Local focus sessions continue to work.",
                        color = Color(0xFF929AAA)
                    )
                }
                email != null -> {
                    Text("Signed in as $email", color = Color(0xFF70D6A1))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                try {
                                    client.auth.signOut()
                                    email = null
                                    message = "Signed out. Your local focus history is unchanged."
                                } catch (error: Exception) {
                                    message = error.message ?: "Could not sign out."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy
                    ) { Text(if (busy) "Please wait…" else "Sign out") }
                }
                else -> {
                    Text(
                        "Sign in to enable cloud account features. The focus timer and history remain available offline.",
                        color = Color(0xFF929AAA)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                try {
                                    client.auth.signInWith(Google)
                                } catch (error: Exception) {
                                    message = error.message ?: "Google sign-in could not be started."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy
                    ) { Text(if (busy) "Opening Google…" else "Continue with Google") }
                }
            }
            message?.let { Text(it, color = Color(0xFFFFB4AB)) }
        }
    }
}
