package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

private fun resolveFirestoreDbId(context: Context): String {
    val appContext = context.applicationContext
    val resId = appContext.resources.getIdentifier(
        "firestore_database_id",
        "string",
        appContext.packageName
    )
    return if (resId != 0) {
        appContext.getString(resId)
    } else {
        "ai-studio-android-nexuscor-499343c5-860f-4fd8-83d6-826859fc6d08"
    }
}

class AstrafolioCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(resolveFirestoreDbId(context))
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Foydalanuvchi Google orqali tizimga kirgan bo'lishi shart.")
    }

    fun observeMissions(): Flow<List<JarvisCloudMission>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/missions"
        emitAll(
            db.collection("users").document(uid).collection("missions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JarvisCloudMission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createMission(
        prompt: String,
        mode: String,
        finalSpeech: String,
        architectReport: String,
        strategistPlan: String,
        sentinelSecurityNote: String,
        creativeSpark: String,
        executedDeviceAction: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val missionId = "mission_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("missions").document(missionId)
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("prompt", prompt.take(2000).ifBlank { "Ovozli buyruq" })
            put("mode", mode)
            put("finalSpeech", finalSpeech.take(6000).ifBlank { "Bajarildi" })
            put("architectReport", architectReport.take(4000).ifBlank { "Tahlil yakunlandi" })
            put("strategistPlan", strategistPlan.take(4000).ifBlank { "Reja tuzildi" })
            put("sentinelSecurityNote", sentinelSecurityNote.take(4000).ifBlank { "Xavfsiz" })
            put("creativeSpark", creativeSpark.take(4000).ifBlank { "Innovatsion yechim" })
            if (!executedDeviceAction.isNullOrBlank()) {
                put("executedDeviceAction", executedDeviceAction.take(500))
            }
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.set(payload).await()
            Result.success(missionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getMissionById(targetUserId: String, missionId: String): Result<JarvisCloudMission?> {
        val docRef = db.collection("users").document(targetUserId).collection("missions").document(missionId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    JarvisCloudMission::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMission(missionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("missions").document(missionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCallLogs(): Flow<List<AutoCallReplyLog>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/call_logs"
        emitAll(
            db.collection("users").document(uid).collection("call_logs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AutoCallReplyLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createCallLog(
        callerNumber: String,
        callerLabel: String,
        statusPreset: String,
        aiGeneratedReply: String,
        autoAnswered: Boolean
    ): Result<String> {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("call_logs").document(callId)
        val payload = mapOf(
            "userId" to uid,
            "callerNumber" to callerNumber.take(100).ifBlank { "Noma'lum raqam" },
            "callerLabel" to callerLabel.take(150).ifBlank { "Kiruvchi qo'ng'iroq" },
            "statusPreset" to statusPreset.take(100).ifBlank { "Bandman" },
            "aiGeneratedReply" to aiGeneratedReply.take(2000).ifBlank { "Assalomu alaykum, hozir bandman." },
            "autoAnswered" to autoAnswered,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(callId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCallLog(callId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("call_logs").document(callId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeVoiceNotes(): Flow<List<VoiceNoteItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/voice_notes"
        emitAll(
            db.collection("users").document(uid).collection("voice_notes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VoiceNoteItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createVoiceNote(title: String, content: String): Result<String> {
        val uid = requireUserId()
        val noteId = "note_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("voice_notes").document(noteId)
        val payload = mapOf(
            "userId" to uid,
            "title" to title.take(300).ifBlank { "Ovozli qayd" },
            "content" to content.take(4000).ifBlank { "Bo'sh qayd" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(noteId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteVoiceNote(noteId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("voice_notes").document(noteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
