package com.focusforge.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Email
import kotlinx.coroutines.launch

private val loginAccent = Color(0xFF9B8CFF)
private val loginMuted = Color(0xFF929AAA)
private val loginError = Color(0xFFFFB4AB)

@Composable
fun LoginRequiredScreen(isConfigured: Boolean) {
    val client = FocusForgeSupabase.clientOrNull()
    val scope = rememberCoroutineScope()
    var isCreatingAccount by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = loginAccent,
        background = Color(0xFF090B10),
        surface = Color(0xFF141923),
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "FOCUSFORGE",
                color = loginAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isCreatingAccount) "Create your account." else "Welcome back.",
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to continue. Your account keeps your FocusForge experience connected.",
                color = loginMuted,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isConfigured || client == null) {
                        Text(
                            "Authentication is not configured for this build. Add the FocusForge Supabase URL and publishable key, then rebuild.",
                            color = loginError
                        )
                    } else {
                        Text(
                            if (isCreatingAccount) "New to FocusForge? Create an account." else "Enter your account details.",
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; message = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; message = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        if (isCreatingAccount) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; message = null },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Confirm password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                            Text(
                                "Enter the same password again to verify it.",
                                color = loginMuted,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                val cleanEmail = email.trim()
                                when {
                                    cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                                        message = "Enter a valid email address."
                                        isError = true
                                    }
                                    password.length < 8 -> {
                                        message = "Use a password with at least 8 characters."
                                        isError = true
                                    }
                                    isCreatingAccount && password != confirmPassword -> {
                                        message = "The passwords do not match. Please check both fields."
                                        isError = true
                                    }
                                    else -> scope.launch {
                                        busy = true
                                        message = null
                                        try {
                                            if (isCreatingAccount) {
                                                client.auth.signUpWith(Email) {
                                                    email = cleanEmail
                                                    password = password
                                                }
                                                message = "Account created. Check your email for a confirmation link if email confirmation is enabled."
                                                isError = false
                                            } else {
                                                client.auth.signInWith(Email) {
                                                    email = cleanEmail
                                                    password = password
                                                }
                                                message = "Signed in successfully."
                                                isError = false
                                            }
                                        } catch (e: Exception) {
                                            message = e.message ?: "Authentication failed. Please try again."
                                            isError = true
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (busy) CircularProgressIndicator()
                            else Text(if (isCreatingAccount) "Create account" else "Sign in")
                        }
                        TextButton(
                            onClick = {
                                isCreatingAccount = !isCreatingAccount
                                password = ""
                                confirmPassword = ""
                                message = null
                            },
                            enabled = !busy
                        ) {
                            Text(
                                if (isCreatingAccount) "Already have an account? Sign in"
                                else "First time here? Create an account"
                            )
                        }
                        message?.let {
                            Text(it, color = if (isError) loginError else Color(0xFF70D6A1), fontSize = 12.sp)
                        }
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
            CircularProgressIndicator(color = loginAccent)
            Spacer(Modifier.height(16.dp))
            Text("Checking your account…", color = Color.White)
        }
    }
}
