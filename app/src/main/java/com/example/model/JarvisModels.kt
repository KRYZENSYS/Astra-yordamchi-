package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisViolet

enum class AiAgentRole(
    val codeName: String,
    val titleUz: String,
    val specialtyUz: String,
    val modelTag: String,
    val accentColor: Color
) {
    CORE(
        codeName = "JARVIS-CORE",
        titleUz = "Ovozli Orkestrator & Telefon Boshqaruvchisi",
        specialtyUz = "Niyatni aniqlash, telefon apparat buyruqlarini bajarish va ovozli sintez",
        modelTag = "Gemini 3.8 Flash + Voice OS",
        accentColor = JarvisCyan
    ),
    ARCHITECT(
        codeName = "NEXUS-CODE",
        titleUz = "Texnik & Kod Arxitektori",
        specialtyUz = "Dasturiy arxitektura, API, avtomatlashtirish va algoritmlar",
        modelTag = "Gemini 3.8 Flash (Tech)",
        accentColor = JarvisElectricBlue
    ),
    STRATEGIST(
        codeName = "STRAT-MIND",
        titleUz = "Tahlil va Strategiya AIsi",
        specialtyUz = "Murakkab rejalashtirish, bosqichma-bosqich yechim va xavf tahlili",
        modelTag = "Gemini 3.8 Flash (Reasoning)",
        accentColor = JarvisAmberGold
    ),
    SENTINEL(
        codeName = "AEGIS-GUARD",
        titleUz = "Xavfsizlik va Telemetriya AIsi",
        specialtyUz = "Kiberxavfsizlik, apparat datchiklari, batareya/RAM nazorati va himoya",
        modelTag = "Gemini 3.5 Flash-Lite + Sensors",
        accentColor = JarvisEmerald
    ),
    CREATIVE(
        codeName = "VISION-NOVA",
        titleUz = "Multimodal Ko'rish va Innovatsiya AIsi",
        specialtyUz = "Rasm/hujjat/kod tahlili, golografik UX va kelajak g'oyalari",
        modelTag = "Gemini 3.8 Multimodal",
        accentColor = JarvisViolet
    )
}

enum class VoicePersona(
    val labelUz: String,
    val descriptionUz: String,
    val pitch: Float,
    val speechRate: Float,
    val promptInstruction: String
) {
    TACTICAL(
        labelUz = "Taktik Qo'mondon",
        descriptionUz = "Aniq, qisqa va harbiy-texnik aniqlikdagi ovozli javoblar",
        pitch = 0.92f,
        speechRate = 1.08f,
        promptInstruction = "Javobing aniq, taktik, qisqa va yuqori texnologik JARVIS uslubida bo'lsin."
    ),
    ARCHITECT_DEEP(
        labelUz = "Ilmiy Arxitektor",
        descriptionUz = "Chuqur texnik, kod va tizim arxitekturasi tafsilotlari bilan",
        pitch = 0.88f,
        speechRate = 0.98f,
        promptInstruction = "Chuqur muhandislik va ko'p-agentli AI arxitekturasi nuqtai nazaridan tahlil qil."
    ),
    VISIONARY(
        labelUz = "Futuristik Vizyoner",
        descriptionUz = "Innovatsion g'oyalar, golografik interfeys va kelajak texnologiyalari",
        pitch = 1.0f,
        speechRate = 1.02f,
        promptInstruction = "Kelajak texnologiyalari, innovatsion yondashuv va kreativ g'oyalar bilan boyitib javob ber."
    )
}

enum class CouncilMode(val labelUz: String) {
    SWARM_AND_CONTROL("5-AI Kengash + Telefon Boshqaruvi"),
    AI_DEBATE("AI Agentlar Bahsi (Debate)"),
    SINGLE_STREAM("Yakka Agent Oqimi (Stream)")
}

enum class VisionAnalysisMode(
    val labelUz: String,
    val promptHint: String
) {
    AUTO_DIAGNOSTICS(
        labelUz = "Avto-Diagnostika & Obyekt",
        promptHint = "Rasmdagi barcha obyektlarni, texnik qurilmalarni yoki holatni to'liq diagnostika qil."
    ),
    CODE_AND_BUGS(
        labelUz = "Kod & Xatolar Tahlili",
        promptHint = "Rasmdagi dastur kodi, terminal xatosi yoki algoritmni o'qib, xatoni va to'g'ri yechimni ko'rsat."
    ),
    DOCUMENT_BRIEFING(
        labelUz = "Hujjat & Matn Brifingi",
        promptHint = "Rasmdagi hujjat, kitob sahifasi yoki jadvalning eng muhim nuqtalarini ajratib, qisqa brifing ber."
    ),
    ARCHITECTURE_DIAGRAM(
        labelUz = "Sxema & Arxitektura",
        promptHint = "Rasmdagi tizim sxemasi, dizayn yoki elektron chizmani muhandislik nuqtai nazaridan tahlil qil."
    )
}

enum class TacticalProtocol(
    val codeName: String,
    val titleUz: String,
    val subtitleUz: String,
    val accentColor: Color
) {
    MORNING_BRIEFING(
        codeName = "PROTOCOL: DAWN-01",
        titleUz = "Kunlik Ovozli Brifing",
        subtitleUz = "Qurilma holati, kunlik strategiya va ustuvor vazifalar hisoboti",
        accentColor = JarvisCyan
    ),
    SYSTEM_DIAGNOSTICS(
        codeName = "PROTOCOL: SYS-SCAN",
        titleUz = "Chuqur Tizim Diagnostikasi",
        subtitleUz = "Batareya, harorat, RAM, xotira va sensorlarning AI auditi",
        accentColor = JarvisEmerald
    ),
    DEEP_FOCUS(
        codeName = "PROTOCOL: HYPER-FOCUS",
        titleUz = "Chuqur Kodlash va Diqqat Rejimi",
        subtitleUz = "Ovozni pasaytirish va maksimal samaradorlik algoritmini yoqish",
        accentColor = JarvisElectricBlue
    ),
    RED_ALERT(
        codeName = "PROTOCOL: AEGIS-SHIELD",
        titleUz = "Red Alert (Xavfsizlik Qalqoni)",
        subtitleUz = "Favqulodda himoya, fonarik signali va kiberxavfsizlik tekshiruvi",
        accentColor = JarvisCrimson
    )
}

data class MultiAiCouncilResponse(
    val userCommand: String,
    val detectedIntent: String,
    val deviceAction: String,
    val actionParameter: String,
    val executedActionStatus: String?,
    val voiceSpokenSummary: String,
    val architectReport: String,
    val strategistPlan: List<String>,
    val sentinelSecurityNote: String,
    val creativeSpark: String,
    val confidenceScore: Int
)

data class AiDebateResponse(
    val topic: String,
    val techThesis: String,
    val strategyCounter: String,
    val securityAudit: String,
    val creativeBreakthrough: String,
    val coreVerdict: String,
    val winnerAgent: String
)

data class VisionHudResult(
    val modeLabel: String,
    val detectedCategory: String,
    val voiceSummary: String,
    val keyFindings: List<String>,
    val technicalDeepDive: String,
    val recommendedActions: List<String>,
    val confidenceScore: Int
)

data class DeviceTelemetrySnapshot(
    val batteryPercent: Int = 100,
    val temperatureCelsius: Float = 28.5f,
    val voltageMv: Int = 4100,
    val isCharging: Boolean = false,
    val usedRamMb: Long = 2048,
    val totalRamMb: Long = 4096,
    val ramUsagePercent: Int = 50,
    val freeStorageGb: Float = 32.0f,
    val totalStorageGb: Float = 64.0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.8f,
    val gForceMagnitude: Float = 9.8f,
    val azimuthDeg: Float = 0f
)

data class JarvisBlueprintIdea(
    val id: String,
    val title: String,
    val category: String,
    val voicePipeline: String,
    val aiModelsUsed: List<String>,
    val coreFeatures: List<String>,
    val implementationSteps: List<String>,
    val powerLevel: Int = 98
)

object CuratedJarvisBlueprints {
    val defaultBlueprints = listOf(
        JarvisBlueprintIdea(
            id = "bp_voice_orchestrator",
            title = "1. Neyron Ovozli Orkestrator (Zero-Latency Voice Core)",
            category = "Ovoz & Muloqot",
            voicePipeline = "Wake-Word ('Hey Jarvis') -> Streaming STT -> Gemini Router -> Neural TTS (<400ms)",
            aiModelsUsed = listOf(
                "Porcupine / OpenWakeWord (Oflayn uyg'otish so'zi)",
                "Gemini 3.8 Flash Live (Asosiy muloqot va niyatni tahlil qilish)",
                "Gemini 3.1 Flash TTS / Android Neural TTS (Tabiiy emotsional ovoz)",
                "Silero VAD (Gapirish tugaganini millisekundda sezish)"
            ),
            coreFeatures = listOf(
                "Full-Duplex Interrupt: Jarvis gapirayotganda uni bo'lib yangi buyruq bersangiz darhol tinglashga o'tadi.",
                "Ovozli Biometriya (Speaker Verification): Faqat sizning ovoz tembringizni taniydi va begonalarga maxfiy ma'lumot bermaydi.",
                "Emotsional Moslashuv: Ovozingizdagi charchoq yoki shoshilinchlikni sezib, javob uzunligini avtomatik moslashtiradi."
            ),
            implementationSteps = listOf(
                "1-qadam: Qurilmada doimiy eshituvchi yengil Wake-Word modulini o'rnatish (batareyani tejash uchun DSP protsessorida).",
                "2-qadam: WebSockets yoki gRPC orqali audio oqimni bo'laklab (chunking) AI modelga uzatish.",
                "3-qadam: AI javobining birinchi jumlasi tayyor bo'lishi bilan TTS sintezini boshlash (to'liq matnni kutmasdan)."
            ),
            powerLevel = 99
        ),
        JarvisBlueprintIdea(
            id = "bp_multi_agent_swarm",
            title = "2. 5-Agentli 'Kengash' Tizimi (Swarm Intelligence)",
            category = "Multi-AI Arxitektura",
            voicePipeline = "Ovozli Buyruq -> Master Router AI -> 4 ta Parallel Mutaxassis AI -> Yagona Xulosa",
            aiModelsUsed = listOf(
                "Router AI (Gemini 3.5 Flash-Lite) — vazifani 100ms ichida agentlarga bo'lib beradi",
                "Coder AI (Gemini 4 Argon / Flash) — kod yozish, terminal va API skriptlarni bajarish",
                "Research AI (Search Grounding) — real vaqtdagi yangiliklar va faktlarni tekshirish",
                "Local Edge AI (Gemma On-Device) — internet o'chganda ham tizimni boshqarish"
            ),
            coreFeatures = listOf(
                "Parallel Bajarish: Siz 'Ertaga uchrashuvim uchun taqdimot tayyorla va ob-havoni ayt' desangiz, 2 ta AI bir vaqtda ishlaydi.",
                "O'z-o'zini Tekshirish (Critic Agent): Bitta AI yozgan kod yoki rejani ikkinchi 'Tanqidchi AI' xatolarga tekshirib keyin sizga taqdim etadi.",
                "Avtonom Asboblar (Function Calling): Telefon fonarigi, budilnik, ovoz, Wi-Fi va ilovalarni bevosita boshqaradi."
            ),
            implementationSteps = listOf(
                "1-qadam: JSON Schema asosida Master Orchestrator promptini va Function Calling interfeyslarini yaratish.",
                "2-qadam: Har bir mutaxassis AI uchun alohida tizim ko'rsatmasi (System Instruction) va xotira kontekstini ajratish.",
                "3-qadam: Agentlarning javoblarini yig'ib, foydalanuvchiga 2-3 jumlalik aniq ovozli brifing qaytarish."
            ),
            powerLevel = 100
        ),
        JarvisBlueprintIdea(
            id = "bp_vision_hud",
            title = "3. Multimodal Ko'z va Golografik HUD (Vision + AR)",
            category = "Ko'rish & Kamera",
            voicePipeline = "'Jarvis, bunga qara' -> Rasm/Kamera kadri + Ovoz -> Gemini Multimodal -> Ovozli Tahlil",
            aiModelsUsed = listOf(
                "Gemini 3.8 Flash Multimodal (Tasvir, hujjat, sxema va kodni tahlil qilish)",
                "ML Kit Object & Text Detection (Real vaqtda kadrda obyektlarni belgilash)",
                "Gemini Nano Banana 2.1 (Chizmalar va vizual sxemalarni generatsiya qilish)"
            ),
            coreFeatures = listOf(
                "Jonli Diagnostika: Telefon kamerasini buzilgan texnika, elektron plata yoki monitor ekranidagi xatoga tutib 'Muammo nimada?' deb so'rash.",
                "Hujjat va Kitob Brifingi: Stol ustidagi shartnoma yoki kitob sahifasiga qarab, eng muhim 3 ta bandni ovozda aytib berish.",
                "AR Ko'zoynak Integratsiyasi: Bluetooth ko'zoynak yoki quloqchin orqali ko'chadagi obyektlar haqida quloqqa ma'lumot berish."
            ),
            implementationSteps = listOf(
                "1-qadam: Photo Picker yoki CameraX orqali yuqori sifatli kadr olish.",
                "2-qadam: Kadrni optimallashtirib (<2MB) ovozli savol bilan birgalikda Gemini Multimodal modeliga yuborish.",
                "3-qadam: Ekranda obyektni HUD nishoni bilan ajratib ko'rsatish va ovozli yechimni o'qib eshittirish."
            ),
            powerLevel = 96
        ),
        JarvisBlueprintIdea(
            id = "bp_episodic_memory",
            title = "4. Cheksiz Xotira va Proaktiv Intellekt (RAG Memory Core)",
            category = "Xotira & Avtomatika",
            voicePipeline = "Kunlik Suhbatlar -> Vector DB (Embeddings) -> Proaktiv Ovozli Eslatma",
            aiModelsUsed = listOf(
                "Text Embedding Model (Suhbatlarni vektorli ma'lumotga aylantirish)",
                "SQLite / Room + Vector Search (Qurilmadagi shifrlangan xotira bazasi)",
                "Gemini 3.8 Flash (Oldingi xotiralarni hozirgi vaziyat bilan bog'lash)"
            ),
            coreFeatures = listOf(
                "Proaktiv Tashabbus: Jarvis siz so'ramasangiz ham: 'Janob, 2 hafta oldin aytgan loyiha muddati ertaga tugaydi, hisobotni tayyorlaymi?' deydi.",
                "Shaxsiy Kontekst Grafigi: Do'stlaringiz, loyihalaringiz, odatlaringiz va maqsadlaringizni o'zaro bog'langan grafikda saqlaydi.",
                "Ertalabki Ovozli Brifing: Uyg'onganingizda ob-havo, kunlik vazifalar, muhim xabarlar va AI tavsiyalarini 60 soniyada o'qib beradi."
            ),
            implementationSteps = listOf(
                "1-qadam: Har bir suhbatdan so'ng 'Muhim faktlarni ajratuvchi' kichik AI agentni ishga tushirish.",
                "2-qadam: Faktlarni vaqt tamg'asi va mavzu bo'yicha mahalliy ma'lumotlar bazasiga yozish.",
                "3-qadam: Yangi savol berilganda bazadan eng yaqin 5 ta xotirani topib, AI promptiga avtomatik qo'shish."
            ),
            powerLevel = 97
        ),
        JarvisBlueprintIdea(
            id = "bp_iot_pc_control",
            title = "5. Kompyuter, Telefon va Aqlli Uy Boshqaruvi (Omni-Control)",
            category = "Ekotizim & IoT",
            voicePipeline = "Mobil Ovoz -> Android Intents & MQTT -> Telefon, Kompyuter & Smart Home",
            aiModelsUsed = listOf(
                "Gemini Function Calling Agent (Tabiiy gapni tizim komandasiga aylantirish)",
                "Android System Intent Bridge (Fonarik, Ovoz, Budilnik, Sozlamalar)",
                "Home Assistant / MQTT Bridge (Chiroq, konditsioner, qulflar va datchiklar)"
            ),
            coreFeatures = listOf(
                "Telefondan To'liq Ovozli Boshqaruv: 'Fonarikni yoq', 'Soat 7:00 ga budilnik qo'y', 'Ovozni ko'tar' buyruqlarini darhol bajarish.",
                "Senariyli Protokollar: 'Chuqur Diqqat rejimini yoq' deyilganda — telefon ovozini pasaytiradi va ish rejasini tuzadi.",
                "Xavfsizlik Tasdiqlovi (Voice PIN): Muhim amallar uchun qo'shimcha ovozli tasdiqlov so'raydi."
            ),
            implementationSteps = listOf(
                "1-qadam: AI javob sxemasiga tizim komandalari (TORCH, ALARM, VOLUME, SETTINGS) kodlarini bog'lash.",
                "2-qadam: Android CameraManager, AudioManager va Intent API-lari orqali komandani real vaqtda bajarish.",
                "3-qadam: Bajarilgan amal natijasini foydalanuvchiga ovozli tasdiqlash."
            ),
            powerLevel = 98
        ),
        JarvisBlueprintIdea(
            id = "bp_termux_apk_builder",
            title = "6. Termux Orqali Avtonom APK Yig'ish va Lokal Server (On-Device Builder)",
            category = "Termux & DevOps",
            voicePipeline = "Termux Shell -> OpenJDK 21 + ARM64 AAPT2 + Gradle -> Astrafolio-Jarvis.apk",
            aiModelsUsed = listOf(
                "AQ. AI Studio Multi-API Cluster (Gemini 3.8 Flash / 3.1 Pro)",
                "Astrafolio Local HTTP Server (:8080 REST & OpenAI Proxy)",
                "Termux Native ARM64 AAPT2 & Android SDK 36 Toolchain"
            ),
            coreFeatures = listOf(
                "Repozitoriy ichidagi tayyor skriptlar: './termux-setup-sdk.sh' va './termux-build-apk.sh' orqali kompyutersiz telefonning o'zida APK yig'ish.",
                "ARM64 AAPT2 Avto-Moslashuv: Termux'da x86_64 xatoligini chetlab o'tib, mahalliy ARM64 aapt2 binar fayli bilan 100% toza APK chiqarish.",
                "Tayyor APK'ni 1-komanda bilan '/sdcard/Download/Astrafolio-Jarvis.apk' papkasiga nusxalash va o'rnatish."
            ),
            implementationSteps = listOf(
                "1-qadam: Termux'da 'pkg install git openjdk-21 gradle aapt2 -y' orqali asosiy paketlarni o'rnatish.",
                "2-qadam: 'bash termux-setup-sdk.sh' skriptini ishga tushirib Android SDK 36 ni avtomatik sozlash.",
                "3-qadam: 'bash termux-build-apk.sh' buyrug'i bilan APK yig'ish va Download papkasidan o'rnatish."
            ),
            powerLevel = 100
        )
    )

    val quickVoiceMissions = listOf(
        "Fonarikni yoq va menga tungi kodlash uchun 3 ta eng muhim qoidani ayt",
        "Ovozni 80 foizga ko'tar va 5 ta AI agentdan iborat Jarvis arxitekturasini tushuntir",
        "Soat 07:30 ga budilnik qo'y va ertalabki samarali kun rejasini tuzib ber",
        "Telefon va kompyuterni ovoz orqali birgalikda boshqaradigan xavfsiz tizim loyihasini ber"
    )

    val quickDebateTopics = listOf(
        "Jarvis uchun qaysi biri muhimroq: 100% Oflayn Lokal AI yoki Bulutli Super-AI?",
        "Ovozli boshqaruv xavfsizligi: Qulaylik ustunmi yoki Qattiq Biometrik Cheklovlar?",
        "Kelajakda dasturchilar kodni klaviaturada yozadimi yoki ovozli AI-arxitektorlar orqali?"
    )
}
