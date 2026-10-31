package com.focusforge.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val groupPanel = Color(0xFF141923)
private val groupAccent = Color(0xFF9B8CFF)
private val groupMuted = Color(0xFF929AAA)

@Serializable
private data class CreateGroupParams(
    @SerialName("p_name") val name: String,
    @SerialName("p_description") val description: String? = null
)

@Serializable
private data class JoinGroupParams(
    @SerialName("p_invite_code") val inviteCode: String
)

@Serializable
private data class GroupRpcResult(
    @SerialName("group_id") val groupId: String,
    @SerialName("invite_code") val inviteCode: String? = null
)

@Serializable
private data class GroupMembership(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String,
    @SerialName("display_name") val displayName: String = "Student"
)

@Serializable
private data class GroupRecord(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null
)

@Serializable
private data class GroupFocusRow(
    @SerialName("user_id") val userId: String,
    @SerialName("status") val status: String,
    @SerialName("ends_at") val endsAt: String? = null
)

private data class GroupMemberDisplay(
    val name: String,
    val role: String,
    val status: String,
    val endsAt: String?
)

@Serializable
private data class GroupInviteRecord(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("group_id") val groupId: String
)

private data class MyGroup(
    val group: GroupRecord,
    val role: String,
    val inviteCode: String?,
    val members: List<GroupMemberDisplay>
)

@Composable
fun GroupsScreen(userId: String, modifier: Modifier = Modifier) {
    val client = FocusForgeSupabase.clientOrNull()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<MyGroup>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(userId) { while (true) { kotlinx.coroutines.delay(10000); refresh = !refresh } }

    LaunchedEffect(userId, refresh) {
        if (client == null) {
            error = "Supabase is not configured."
            loading = false
        } else {
            loading = true
            error = null
            try {
                groups = withContext(Dispatchers.IO) {
                    val memberships = client.from("group_members").select {
                        filter { eq("user_id", userId) }
                    }.decodeList<GroupMembership>()
                    memberships.mapNotNull { membership ->
                        val group = client.from("groups").select {
                            filter { eq("id", membership.groupId) }
                        }.decodeList<GroupRecord>().firstOrNull() ?: return@mapNotNull null
                        val invite = if (membership.role == "owner" || membership.role == "admin") {
                            client.from("group_invites").select {
                                filter { eq("group_id", membership.groupId) }
                            }.decodeList<GroupInviteRecord>().firstOrNull()?.inviteCode
                        } else null
                        val memberRows = client.from("group_members").select {
                            filter { eq("group_id", membership.groupId) }
                        }.decodeList<GroupMembership>()
                        val focusRows = client.from("group_focus_status").select {
                            filter { eq("group_id", membership.groupId) }
                        }.decodeList<GroupFocusRow>()
                        val memberDisplays = memberRows.map { member ->
                            val focus = focusRows.firstOrNull { it.userId == member.userId }
                            val status = when {
                                focus == null -> "Not focusing"
                                focus.status == "focusing" && focus.endsAt != null &&
                                    runCatching { java.time.Instant.parse(focus.endsAt).isAfter(java.time.Instant.now()) }.getOrDefault(false) -> "Focusing"
                                focus.status == "focusing" -> "Not focusing"
                                focus.status == "paused" -> "Paused"
                                focus.status == "completed" -> "Completed"
                                else -> "Not focusing"
                            }
                            GroupMemberDisplay(member.displayName, member.role, status, focus?.endsAt)
                        }
                        MyGroup(group, membership.role, invite, memberDisplays)
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not load your groups."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Groups", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Study alongside people you choose. Every member keeps an independent focus timer.", color = groupMuted, fontSize = 13.sp)

        Card(colors = CardDefaults.cardColors(containerColor = groupPanel)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Create a group", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (optional)") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        val name = groupName.trim()
                        if (name.length !in 2..60) {
                            error = "Group name must be 2–60 characters."
                        } else if (client == null) {
                            error = "Supabase is not configured."
                        } else scope.launch {
                            busy = true
                            error = null
                            message = null
                            try {
                                val result = client.postgrest.rpc(
                                    "create_group",
                                    CreateGroupParams(name, description.trim().ifBlank { null })
                                ).decodeList<GroupRpcResult>().first()
                                groupName = ""
                                description = ""
                                message = "Group created. Invitation code: ${result.inviteCode ?: "unavailable"}"
                                refresh = !refresh
                            } catch (e: Exception) {
                                error = e.message ?: "Could not create the group."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) CircularProgressIndicator() else Text("Create group")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = groupPanel)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Join a group", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase().filter(Char::isLetterOrDigit).take(8); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("8-character invitation code") },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = {
                        if (joinCode.length != 8) {
                            error = "Enter the complete 8-character invitation code."
                        } else if (client == null) {
                            error = "Supabase is not configured."
                        } else scope.launch {
                            busy = true
                            error = null
                            message = null
                            try {
                                client.postgrest.rpc(
                                    "join_group_by_code",
                                    JoinGroupParams(joinCode)
                                ).decodeList<GroupRpcResult>()
                                joinCode = ""
                                message = "You joined the group."
                                refresh = !refresh
                            } catch (e: Exception) {
                                error = e.message ?: "Could not join the group."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join group") }
            }
        }

        error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 12.sp) }
        message?.let { Text(it, color = Color(0xFF70D6A1), fontSize = 12.sp) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Your groups", color = Color.White, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { refresh = !refresh }, enabled = !loading && !busy) { Text("Refresh") }
        }

        when {
            loading -> CircularProgressIndicator(color = groupAccent)
            groups.isEmpty() -> Text("You haven't joined any groups yet.", color = groupMuted, fontSize = 13.sp)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups, key = { it.group.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = groupPanel)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(item.group.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            item.group.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = groupMuted, fontSize = 12.sp)
                            }
                            Text("Role: ${item.role}", color = groupAccent, fontSize = 11.sp)
                            HorizontalDivider(color = Color(0xFF303746))
                            Text("Members", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            item.members.forEach { member ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(member.name + if (member.role == "owner") " · Owner" else "", color = Color.White, fontSize = 12.sp)
                                    Text(member.status, color = when (member.status) {
                                        "Focusing" -> Color(0xFF70D6A1)
                                        "Paused" -> Color(0xFFE8B96C)
                                        else -> groupMuted
                                    }, fontSize = 11.sp)
                                }
                            }
                            item.inviteCode?.let { code ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Invite code: $code", color = Color.White, fontSize = 13.sp)
                                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                                        Text("Copy code")
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
