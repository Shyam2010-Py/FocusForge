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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EducationProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("education_level") val educationLevel: String,
    @SerialName("institution_name") val institutionName: String,
    @SerialName("stream") val stream: String,
    @SerialName("academic_year") val academicYear: String,
    @SerialName("semester") val semester: String
)

@Serializable
data class StudentNameRecord(
    @SerialName("display_name") val displayName: String
)

private val onboardingBg = Color(0xFF090B10)
private val onboardingPanel = Color(0xFF141923)
private val onboardingAccent = Color(0xFF9B8CFF)
private val onboardingMuted = Color(0xFF929AAA)

@Composable
fun StudentOnboardingScreen(
    userId: String,
    editMode: Boolean = false,
    onCompleted: () -> Unit
) {
    val client = FocusForgeSupabase.clientOrNull()
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("Diploma") }
    var branch by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadingProfile by remember { mutableStateOf(editMode) }

    LaunchedEffect(userId, editMode) {
        if (editMode && client != null) {
            try {
                val profile = withContext(Dispatchers.IO) {
                    client.from("education_profiles").select {
                        filter { eq("user_id", userId) }
                    }.decodeList<EducationProfile>().firstOrNull()
                }
                val name = withContext(Dispatchers.IO) {
                    client.from("profiles").select {
                        filter { eq("id", userId) }
                    }.decodeList<StudentNameRecord>().firstOrNull()
                }
                fullName = name?.displayName.orEmpty()
                institution = profile?.institutionName.orEmpty()
                educationLevel = profile?.educationLevel ?: "Diploma"
                branch = profile?.stream.orEmpty()
                academicYear = profile?.academicYear.orEmpty()
                semester = profile?.semester.orEmpty()
            } catch (e: Exception) {
                error = e.message ?: "Could not load your saved profile."
            } finally {
                loadingProfile = false
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = onboardingAccent,
            background = onboardingBg,
            surface = onboardingPanel,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCDD2DF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FOCUSFORGE", color = onboardingAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text(if (editMode) "Edit your student profile" else "Set up your student profile", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(if (editMode) "Update your academic information." else "Complete these details to personalize your study experience.", color = onboardingMuted, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = onboardingPanel)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    if (loadingProfile) CircularProgressIndicator(color = onboardingAccent)
                    OutlinedTextField(fullName, { fullName = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
                    OutlinedTextField(institution, { institution = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("College / Institution name") }, singleLine = true)
                    OutlinedTextField(educationLevel, { educationLevel = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Education level (e.g. Diploma)") }, singleLine = true)
                    OutlinedTextField(branch, { branch = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Branch / Specialization (e.g. Electronics & Communication Engineering)") }, singleLine = true)
                    OutlinedTextField(academicYear, { academicYear = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Current year (e.g. 2nd Year)") }, singleLine = true)
                    OutlinedTextField(semester, { semester = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Current semester (e.g. 3rd Semester)") }, singleLine = true)
                    error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 12.sp) }
                    Button(
                        onClick = {
                            val cleanName = fullName.trim()
                            val cleanInstitution = institution.trim()
                            val cleanLevel = educationLevel.trim()
                            val cleanBranch = branch.trim()
                            val cleanYear = academicYear.trim()
                            val cleanSemester = semester.trim()
                            when {
                                cleanName.isBlank() || cleanInstitution.isBlank() || cleanLevel.isBlank() ||
                                    cleanBranch.isBlank() || cleanYear.isBlank() || cleanSemester.isBlank() ->
                                    error = "Complete every field to continue."
                                client == null -> error = "Supabase is not configured in this build."
                                else -> scope.launch {
                                    busy = true
                                    error = null
                                    try {
                                        client.from("education_profiles").upsert(
                                            EducationProfile(
                                                userId = userId,
                                                educationLevel = cleanLevel,
                                                institutionName = cleanInstitution,
                                                stream = cleanBranch,
                                                academicYear = cleanYear,
                                                semester = cleanSemester
                                            )
                                        )
                                        client.from("profiles").update(
                                            mapOf("display_name" to cleanName)
                                        ) {
                                            filter { eq("id", userId) }
                                        }
                                        onCompleted()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Could not save your profile. Please try again."
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        },
                        enabled = !busy && !loadingProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) CircularProgressIndicator()
                        else Text(if (editMode) "Save changes" else "Save profile and continue")
                    }
                }
            }
        }
    }
}
