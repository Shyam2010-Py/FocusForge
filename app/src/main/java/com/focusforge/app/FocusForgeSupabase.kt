package com.focusforge.app

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth

object FocusForgeSupabase {
    private val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                scheme = "focusforge"
                host = "login"
            }
        }
    }

    fun clientOrNull() = if (configured) client else null

    fun handleDeepLink(intent: Intent?, scope: CoroutineScope) {
        if (!configured || intent?.data == null) return
        scope.launch {
            client.handleDeeplinks(intent)
        }
    }
}
