# Astrafolio — Maksimal Darajadagi Multi-AI va Ovozli Telefon Boshqaruvi

Ushbu yangilanish **Astrafolio** ilovasini haqiqiy **JARVIS** darajasiga olib chiqadi: ovoz orqali telefon funksiyalarini bevosita boshqarish, birlashgan **Multimodal Vision HUD** (rasm, obyekt, hujjat va kod tahlili), **Jonli Qurilma Telemetriyasi va Sensor Radari**, hamda **AI Agentlar Bahsi (Debate Arena)** va **Ovozli Protokollar** qo‘shiladi.

### User Review & Critical Decisions

> [!IMPORTANT]
> Sizning javoblaringiz asosida quyidagi ustuvor qarorlar tasdiqlandi va rejaga kiritildi:

- **Ovoz bilan telefonni bemalol boshqarish (Voice Device Controller)**: Tabiiy o‘zbek tilidagi ovozli buyruqlar (*"Fonarikni yoq"*, *"Soat 07:00 ga budilnik qo‘y"*, *"Ovozni 80 foizga ko‘tar"*, *"Wi-Fi sozlamalarini och"*, *"Google-dan sun'iy intellekt yangiliklarini qidir"*) AI tomonidan tahlil qilinib, Android tizim funksiyalari (`CameraManager` fonarik, `AudioManager` ovoz balandligi, `AlarmClock` budilnik/taymer, `Settings` oynalari, qidiruv va kalendar) orqali avtomatik bajariladi.
- **Yagona Multimodal Vision HUD**: Android Photo Picker orqali rasm, sxema, hujjat yoki kod skrinshotini tanlab, `Gemini 3.8 Flash` multimodal modeli yordamida ham obyekt diagnostikasi, ham kod/hujjat tahlilini bitta golografik interfeysda bajarish va natijani ovozda eshitish.
- **Barcha 4 ta Maksimal Modullar**:
  1. **Ovozli Kengash + Telefon Boshqaruvi + AI Agentlar Bahsi (Debate Mode)**
  2. **Vision HUD (Multimodal Rasm, Hujjat va Kod Tahlili)**
  3. **Telemetriya & Sensor Radari + Ovozli Protokollar / Kunlik Brifing**
  4. **Jarvis G‘oyalar & Xotira Markazi**

---

### 1. Overview & Core Concept

- **What It Does**: Foydalanuvchi ovoz yoki matn orqali nafaqat 5 ta AI agentdan iborat kengash bilan maslahatlashadi, balki telefonning real apparat funksiyalarini ovozda boshqaradi, rasmlar va hujjatlarni AI ko‘zi bilan tahlil qiladi, telefon datchiklari va batareya/xotira holatini jonli radarda kuzatadi hamda AI agentlarning o‘zaro ilmiy-taktik bahsini tinglaydi.
- **Target Audience / Persona**: O‘zbek tilida mukammal, ko‘p-agentli va ovozli shaxsiy JARVIS yordamchisiga ega bo‘lishni istagan texnologiya ixlosmandlari va dasturchilar.
- **Key Value**: Suhbatdosh AI-dan **amaliy bajaruvchi (Action-oriented) va ko‘ruvchi (Multimodal) avtonom tizimga** o‘tish.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Ovozli Telefon Boshqaruvi va Kengash (`Ovozli Kengash` bo‘limi)**:
     - Foydalanuvchi mikrofonni bosib *"Fonarikni yoq va tungi kodlash protokolini ishga tushir"* yoki *"Soat 8:30 ga budilnik qo‘y"* deydi.
     - AI buyruq ichidan **Tizim Amali (Device Action)** bor-yo‘qligini aniqlaydi, fonarikni yoqadi yoki budilnik/sozlama oynasini ishga tushiradi va ovozda *"Janob, fonarik yoqildi va tungi rejim faollashtirildi"* deb tasdiqlaydi.
     - **AI Bahs Rejimi (Debate Mode)** yoqilganda, foydalanuvchi bergan murakkab savol ustida `NEXUS-CODE` (Texnik) va `STRAT-MIND` (Strateg) o‘zaro argumentlar keltirib bahslashadi, `JARVIS-CORE` esa yakuniy hakam xulosasini ovozda o‘qiydi.
  2. **Birlashgan Vision HUD (`Vision Ko‘z` bo‘limi)**:
     - Galereyadan har qanday rasm, elektron plata, kitob sahifasi yoki kod skrinshoti tanlanadi.
     - Tahlil rejimi tanlanadi: *Avto-Diagnostika*, *Hujjat & Matn Xulosasi*, *Kod & Xatolarni Topish*, yoki *Arxitektura Sxemasi*.
     - Ekranda golografik skanerlash animatsiyasi aylanib, topilgan obyektlar, muammolar va yechimlar kartochkalarda chiqadi hamda ovozda o‘qib beriladi.
  3. **Telemetriya Radari va Ovozli Protokollar (`Radar & Boshqaruv` bo‘limi)**:
     - **Jonli Sensor Radari**: Telefonning akselerometr/magnitometr datchiklari asosida aylanuvchi 360° taktik radar, batareya foizi, harorat (°C), kuchlanish (mV), operativ xotira (RAM) va bo‘sh joy ko‘rsatkichlari real vaqtda ko‘rsatiladi.
     - **Tezkor Ovozli Boshqaruv Paneli**: Fonarik (Torch ON/OFF), Ovoz balandligi slayderi, Wi-Fi/Bluetooth/Displey sozlamalari, Budilnik va Taymerni bir bosishda yoki ovozda sinash.
     - **Taktik Protokollar & Kunlik Brifing**: *"Ertalabki Brifing"*, *"Red Alert (Favqulodda Himoya)"*, *"Deep Focus (Chuqur Diqqat)"*, va *"Tizim Diagnostikasi"* tugmalari bosilganda, AI telefonning real telemetriya ko‘rsatkichlarini qo‘shib jonli ovozli hisobot beradi.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Cybernetic Tactical HUD** — qora-ko‘k obsidian fon, neon moviy (`#00E5FF`), kahrabo oltin (`#FFB300`), zumrad yashil (`#10B981`) va binafsha (`#A855F7`) urg‘ular.
  - *Typography*: `Space Grotesk` (HUD sarlavhalar), `JetBrains Mono` (datchik raqamlari, telemetriya va agent kodlari), `Plus Jakarta Sans` (asosiy matn).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: AI-ga asoslangan Tabiiy Ovozli Telefon Boshqaruvi (Intent & Hardware Bridge)**
  - *Chosen Approach*: `Gemini 3.8 Flash` structured JSON chiqishiga maxsus `deviceAction` va `actionParameter` maydonlarini qo‘shamiz (`TORCH_ON`, `TORCH_OFF`, `VOLUME_UP`, `VOLUME_DOWN`, `MUTE_AUDIO`, `SET_ALARM`, `SET_TIMER`, `OPEN_WIFI`, `OPEN_BLUETOOTH`, `OPEN_DISPLAY`, `WEB_SEARCH`, `NONE`).
  - *Why*: Foydalanuvchi qanday jumla tuzishidan qat’i nazar (*"Chiroqni yoqvor"*, *"Fonarik kerak"*, *"Qorong‘u bo‘lib ketdi"*), AI niyatni aniq tushunadi va telefonning haqiqiy apparat/tizim funksiyasini ishga tushiradi.
- **Decision 2: Zero-Permission Rasm Tanlash + Multimodal Gemini 3.8 Flash**
  - *Chosen Approach*: Google Play siyosatiga to‘liq mos ravishda hech qanday ortiqcha xotira ruxsatisiz `PickVisualMedia` orqali rasm tanlanadi, tasvir o‘lchami optimallashtirilib (`< 2MB` Bitmap), `Gemini 3.8 Flash` modeliga uzatiladi.
  - *Why*: Ham obyektlarni, ham hujjat va kod skrinshotlarini yuqori aniqlikda va xavfsiz tahlil qilish imkonini beradi.
- **Decision 3: Haqiqiy Qurilma Telemetriyasi (No Mock Sensors)**
  - *Chosen Approach*: Android `BatteryManager`, `SensorManager` (Accelerometer & Magnetometer), `ActivityManager` (RAM) va `CameraManager` (Flashlight) API-laridan haqiqiy ko‘rsatkichlar olinadi.

---

### 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         ASTRAFOLIO HUD INTERFACE                          │
├─────────────────┬─────────────────┬───────────────────┬───────────────────┤
│ 1. Ovozli       │ 2. Vision HUD   │ 3. Radar &        │ 4. G'oyalar &     │
│    Kengash      │    (Multimodal) │    Telemetriya    │    Xotira         │
│  • 5-AI Swarm   │  • Photo Picker │  • 360° Sensor    │  • AI Blueprint   │
│  • AI Debate    │  • Obyekt/Hujjat│    Radar Canvas   │    Generator      │
│  • Voice Action │    /Kod Tahlili │  • Battery/RAM/°C │  • 5-AI Matrix    │
│    Dispatcher   │  • Ovozli Xulosa│  • Protokollar    │  • Room Tarix     │
└────────┬────────┴────────┬────────┴─────────┬─────────┴─────────┬─────────┘
         │                 │                  │                   │
         ▼                 ▼                  ▼                   ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     JARVIS VIEWMODEL & ORCHESTRATOR                       │
├─────────────────────────────────────┬─────────────────────────────────────┤
│   FIREBASE AI LOGIC (GEMINI 3.8)    │   ANDROID SYSTEM & HARDWARE LAYER   │
│  • Council + DeviceAction Schema    │  • SpeechRecognizer + TextToSpeech  │
│  • AI Debate Schema (3-Round Clash) │  • CameraManager (Flashlight Torch) │
│  • Vision HUD Schema (Bitmap+Text)  │  • AudioManager (Volume / Mute)     │
│  • Protocol & Briefing Generator    │  • AlarmClock / Settings / Web      │
│  • Blueprint Generator Schema       │  • SensorManager + BatteryManager   │
└─────────────────────────────────────┴─────────────────────────────────────┘
```

- **Interactive Component & State Mapping**:
  - **Voice Command & Device Action Executor**: Foydalanuvchi ovozli yoki matnli buyruq yuborganda `JarvisDeviceController` AI qaytargan `deviceAction` kodini tekshiradi, mos Android tizim amalini bajaradi va bajarilgan amal statusini UI kartochkasida hamda ovozli xulosada ko‘rsatadi.
  - **AI Debate Mode Toggle**: `Ovozli Kengash` ekranida *"AI Bahs Rejimi (Debate)"* yoqilganda, savol yuzasidan 3 bosqichli bahs (`Texnik Tezis`, `Strategik Qarshi-Argument`, `Yakuniy Sintez`) generatsiya qilinadi.
  - **Vision HUD Scanner**: Rasm tanlanganda `Bitmap` yuklanadi, foydalanuvchi tahlil turini (*Umumiy*, *Kod/Xato*, *Hujjat*) tanlab *"Vizual Tahlilni Boshlash"* tugmasini bosadi; strukturalangan natija (`detectedObjects`, `technicalAnalysis`, `actionableSolution`, `voiceSummary`) ekranda va ovozda taqdim etiladi.
  - **Live Telemetry & Protocols**: `JarvisTelemetryManager` real vaqtda batareya, harorat, RAM va 3 o‘qli akselerometr ma’lumotlarini `StateFlow` orqali yangilab turadi; maxsus protokol bosilganda ushbu real raqamlar AI brifingiga qo‘shib o‘qib eshittiriladi.
