package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.device.JarvisDeviceController
import com.example.model.AiAgentRole
import com.example.model.CouncilMode
import com.example.model.CuratedJarvisBlueprints
import com.example.model.TacticalProtocol
import com.example.model.VisionAnalysisMode
import com.example.network.AiStudioMultiApiManager
import com.example.network.ApiNodePowerState
import com.example.network.ApiTaskScope
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUpFirebase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("demo-no-project")
                    .setApplicationId("1:1234567890:android:abcdef")
                    .setApiKey("fake-api-key-for-test")
                    .build()
            )
        }
    }

    @Test
    fun `verify astrafolio multi-agent models and multi-api sleep wake manager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Astrafolio", appName)
        assertEquals(5, AiAgentRole.entries.size)
        assertEquals(3, CouncilMode.entries.size)
        assertEquals(4, VisionAnalysisMode.entries.size)
        assertEquals(4, TacticalProtocol.entries.size)
        assertTrue(CuratedJarvisBlueprints.defaultBlueprints.size >= 5)

        val controller = JarvisDeviceController(context)

        // Test 0ms instant Uzbek voice hardware command detection
        val instantOn = controller.tryExecuteInstantHardwareCommand("Jarvis, fonarikni yoq")
        assertNotNull(instantOn)
        assertEquals("TORCH_ON", instantOn?.first)
        assertTrue(controller.isTorchOn.value)

        val instantOff = controller.tryExecuteInstantHardwareCommand("Fonarikni o'chir")
        assertNotNull(instantOff)
        assertEquals("TORCH_OFF", instantOff?.first)
        assertFalse(controller.isTorchOn.value)

        // Test 0ms instant 24/7 background service, auto-call secretary & Multi-API sleep/wake voice commands
        val bgServiceCmd = controller.tryExecuteInstantHardwareCommand("Orqa fon rejimini yoq")
        assertNotNull(bgServiceCmd)
        assertEquals("START_BACKGROUND_MODE", bgServiceCmd?.first)

        val autoCallCmd = controller.tryExecuteInstantHardwareCommand("Avto-kotibni yoq")
        assertNotNull(autoCallCmd)
        assertEquals("ENABLE_AUTO_CALL_RESPONDER", autoCallCmd?.first)

        val sleepApiCmd = controller.tryExecuteInstantHardwareCommand("AI Studio API'larni uxlat")
        assertNotNull(sleepApiCmd)
        assertEquals("SLEEP_STANDBY_APIS", sleepApiCmd?.first)

        val wakeApiCmd = controller.tryExecuteInstantHardwareCommand("AI Studio API'larni uyg'ot")
        assertNotNull(wakeApiCmd)
        assertEquals("WAKE_ALL_APIS", wakeApiCmd?.first)

        val tokenReportCmd = controller.tryExecuteInstantHardwareCommand("Nechta token ishlatildi?")
        assertNotNull(tokenReportCmd)
        assertEquals("SPEAK_TOKEN_REPORT", tokenReportCmd?.first)

        val localServerCmd = controller.tryExecuteInstantHardwareCommand("Lokal serverni yoq")
        assertNotNull(localServerCmd)
        assertEquals("START_LOCAL_SERVER", localServerCmd?.first)

        // Test AI Studio Multi-API Manager with AQ. API keys, local server bridge, and token counting
        val apiManager = AiStudioMultiApiManager(context)
        apiManager.resetTokenCounters(null)
        assertEquals(0L, apiManager.tokenSummary.value.totalTokens)

        val addedAqNode = apiManager.addCustomAiStudioNode(
            label = "AI Studio #4 • AQ. Maxsus",
            apiKey = "AQ.Ab8RN6TestKey123456789",
            modelName = "gemini-3.8-flash",
            taskScope = ApiTaskScope.UNIVERSAL,
            startSleeping = true,
            dailyTokenLimit = 500_000L,
            serverUrl = "http://127.0.0.1:8080"
        )
        assertEquals(ApiNodePowerState.SLEEPING, addedAqNode.powerState)
        assertTrue(addedAqNode.isAqKey())
        assertTrue(addedAqNode.maskedApiKey().startsWith("AQ."))

        // Batch connect multiple AQ. API keys at once
        val batchAddedCount = apiManager.addMultipleApiKeysBatch(
            rawKeysInput = "AQ.BatchKeyAlpha111\nAQ.BatchKeyBeta222",
            modelName = "gemini-3.8-flash",
            taskScope = ApiTaskScope.UNIVERSAL,
            startSleeping = false,
            dailyTokenLimit = 1_000_000L,
            serverUrl = "http://127.0.0.1:8080"
        )
        assertEquals(2, batchAddedCount)
        assertTrue(apiManager.tokenSummary.value.connectedKeysCount >= 3)

        val nodes = apiManager.apiNodes.value
        val nodeA = nodes[nodes.size - 2]
        val nodeB = nodes[nodes.size - 1]
        assertTrue(nodeA.isAqKey())
        assertTrue(nodeB.isAqKey())

        // Configure Local/Internet Server Bridge
        apiManager.configureLocalServerBridge(true, "http://127.0.0.1:8080")
        assertTrue(apiManager.localServerBridgeEnabled.value)

        // Record token usage across multiple AQ. API keys and verify exact aggregation
        apiManager.recordNodeTelemetry(
            nodeId = nodeA.id,
            latencyMs = 120L,
            statusNote = "Test #1",
            promptTokensUsed = 150,
            outputTokensUsed = 350,
            totalTokensUsed = 500
        )
        apiManager.recordNodeTelemetry(
            nodeId = nodeB.id,
            latencyMs = 95L,
            statusNote = "Test #2",
            promptTokensUsed = 200,
            outputTokensUsed = 400,
            totalTokensUsed = 600
        )

        assertEquals(350L, apiManager.tokenSummary.value.totalPromptTokens)
        assertEquals(750L, apiManager.tokenSummary.value.totalOutputTokens)
        assertEquals(1100L, apiManager.tokenSummary.value.totalTokens)
        assertEquals(600, apiManager.tokenSummary.value.lastRequestTokens)

        // Reset token counters and verify 0
        apiManager.resetTokenCounters(null)
        assertEquals(0L, apiManager.tokenSummary.value.totalTokens)

        apiManager.wakeAllApis()
        assertTrue(apiManager.apiNodes.value.all { it.powerState == ApiNodePowerState.ACTIVE })

        apiManager.sleepAllStandbyApis()
        assertEquals(1, apiManager.apiNodes.value.count { it.powerState == ApiNodePowerState.ACTIVE })

        controller.release()
    }

    @Test
    fun `verify google auth screen and 5 HUD tabs navigation`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
    }

    @Test
    fun `verify navigation across all 5 HUD tabs and AI Studio Multi-API Token Cluster in AstrafolioApp`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AstrafolioApp()
            }
        }
        composeTestRule.waitForIdle()

        // 1. Default tab: Voice Council
        composeTestRule.onNodeWithTag("voice_council_list").assertExists()

        // 2. Navigate to Auto-Call Secretary & 24/7 Background tab
        composeTestRule.onNodeWithTag("nav_tab_secretary").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auto_call_secretary_list").assertExists()

        // 3. Navigate to Vision HUD tab
        composeTestRule.onNodeWithTag("nav_tab_vision").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("vision_hud_list").assertExists()

        // 4. Navigate to Radar & Telemetry tab
        composeTestRule.onNodeWithTag("nav_tab_radar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("telemetry_radar_list").assertExists()

        // 5. Navigate to Blueprints, Memory & AI API Cluster tab
        composeTestRule.onNodeWithTag("nav_tab_blueprints").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("blueprints_list").assertExists()

        // Switch to 5-AI Agents & Memory subtab
        composeTestRule.onNodeWithTag("subtab_memory").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("agents_memory_list").assertExists()

        // Switch to AI Studio Multi-API Cluster subtab
        composeTestRule.onNodeWithTag("subtab_api_cluster").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_studio_cluster_list").assertExists()
        composeTestRule.onNodeWithTag("top_bar_api_cluster_chip").assertExists()
        composeTestRule.onNodeWithTag("btn_sleep_standby_apis").assertExists()
        composeTestRule.onNodeWithTag("btn_wake_all_apis").assertExists()
        composeTestRule.onNodeWithTag("btn_toggle_batch_keys_form").assertExists()
    }
}
