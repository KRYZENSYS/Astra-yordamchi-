package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AstrafolioCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createMission_and_createCallLog_validPayload_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = AstrafolioCloudRepository(firestore)

        val missionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMission(
                prompt = "Jarvis, fonarikni yoq va tizimni tekshir",
                mode = "VOICE_COUNCIL",
                finalSpeech = "Fonarik yoqildi va tizim barqaror.",
                architectReport = "CameraManager API orqali torch yoqildi.",
                strategistPlan = "1-bosqich: Chiroqni yoqish.",
                sentinelSecurityNote = "Xavfsizlik me'yorida.",
                creativeSpark = "Ovozli taymer qo'shish.",
                executedDeviceAction = "TORCH_ON"
            )
        }
        assertTrue(missionResult.isSuccess)
        val missionId = missionResult.getOrThrow()
        assertTrue(missionId.isNotBlank())

        val callLogResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCallLog(
                callerNumber = "+998901234567",
                callerLabel = "Sardorbek",
                statusPreset = "Uchrashuvdaman",
                aiGeneratedReply = "Assalomu alaykum! Xo'jayinim hozir muhim uchrashuvda.",
                autoAnswered = true
            )
        }
        assertTrue(callLogResult.isSuccess)
        val callId = callLogResult.getOrThrow()

        val emittedLogs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCallLogs().first { list -> list.any { it.id == callId } }
        }
        assertTrue(emittedLogs.any { it.id == callId && it.callerNumber == "+998901234567" })
    }

    @Test
    fun createVoiceNote_and_crossUserAccess_enforcesIsolation() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AstrafolioCloudRepository(firestore)
        val missionId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createMission(
                prompt = "Maxfiy buyruq",
                mode = "VOICE_COUNCIL",
                finalSpeech = "Maxfiy hisobot",
                architectReport = "Arxitektura",
                strategistPlan = "Strategiya",
                sentinelSecurityNote = "Himoya",
                creativeSpark = "G'oya"
            ).getOrThrow()
        }

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createVoiceNote("Eslatma", "Soat 15:00 da qo'ng'iroq qilish").getOrThrow()
        }
        assertTrue(noteId.isNotBlank())

        // Switch to Bob and verify cross-user read is rejected with PERMISSION_DENIED
        signInTestUser(BOB_EMAIL)
        val bobRepo = AstrafolioCloudRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getMissionById(aliceUid, missionId)
        }
        assertTrue(crossReadResult.isFailure)
        val fsException = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
    }

    @Test
    fun observeMissions_unauthenticatedUser_failsLists() = runBlocking {
        auth.signOut()
        val repository = AstrafolioCloudRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMissions().first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException() != null)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@astrafolio.test"
        const val BOB_EMAIL = "bob@astrafolio.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
