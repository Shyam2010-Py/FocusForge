package com.focusforge.app

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupFocusStatus(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("status") val status: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("planned_seconds") val plannedSeconds: Int? = null
)

@Serializable
private data class GroupFocusMembership(
    @SerialName("group_id") val groupId: String
)

suspend fun publishIndividualFocusStatus(
    userId: String,
    status: String,
    startedAtMillis: Long?,
    endsAtMillis: Long?,
    plannedSeconds: Int?
) {
    val client = FocusForgeSupabase.clientOrNull() ?: return
    val memberships = client.from("group_members").select {
        filter { eq("user_id", userId) }
    }.decodeList<GroupFocusMembership>()

    val startedAt = startedAtMillis?.let {
        java.time.Instant.ofEpochMilli(it).toString()
    }
    val endsAt = endsAtMillis?.let {
        java.time.Instant.ofEpochMilli(it).toString()
    }

    memberships.forEach { membership ->
        client.from("group_focus_status").upsert(
            GroupFocusStatus(
                groupId = membership.groupId,
                userId = userId,
                status = status,
                startedAt = startedAt,
                endsAt = endsAt,
                plannedSeconds = plannedSeconds
            )
        )
    }
}
