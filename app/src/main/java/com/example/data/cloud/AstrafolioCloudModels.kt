package com.example.data.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class JarvisCloudMission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val mode: String = "VOICE_COUNCIL",
    val finalSpeech: String = "",
    val architectReport: String = "",
    val strategistPlan: String = "",
    val sentinelSecurityNote: String = "",
    val creativeSpark: String = "",
    val executedDeviceAction: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AutoCallReplyLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val callerNumber: String = "",
    val callerLabel: String = "",
    val statusPreset: String = "Uchrashuvdaman",
    val aiGeneratedReply: String = "",
    val autoAnswered: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VoiceNoteItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
