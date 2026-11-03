const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write missions, call_logs, or voice_notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("missions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("call_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("voice_notes").get());
});

test("Authenticated user: can create and read own mission, call_log, and voice_note", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("m_1").set({
      userId: ALICE_UID,
      prompt: "Fonarikni yoq va tizim holatini ayt",
      mode: "VOICE_COUNCIL",
      finalSpeech: "Fonarik yoqildi, tizim barqaror.",
      architectReport: "CameraManager torch API chaqirildi.",
      strategistPlan: "1-qadam: Fonarikni yoqish.",
      sentinelSecurityNote: "Xavfsizlik me'yorida.",
      creativeSpark: "Avto-taymer qo'shish mumkin.",
      executedDeviceAction: "Telefon fonarigi YOQILDI",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("call_logs").doc("c_1").set({
      userId: ALICE_UID,
      callerNumber: "+998901234567",
      callerLabel: "Azizbek Hamkor",
      statusPreset: "Uchrashuvdaman",
      aiGeneratedReply: "Assalomu alaykum! Xo'jayinim hozir uchrashuvda, tez orada qayta aloqaga chiqadi.",
      autoAnswered: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("voice_notes").doc("n_1").set({
      userId: ALICE_UID,
      title: "Muhim uchrashuv",
      content: "Ertaga soat 10:00 da loyiha taqdimoti",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("call_logs").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("voice_notes").get()
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's missions or call logs", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("call_logs").doc("c_secret").set({
      userId: ALICE_UID,
      callerNumber: "+998901112233",
      callerLabel: "Maxfiy Qo'ng'iroq",
      statusPreset: "Bandman",
      aiGeneratedReply: "Avtomatik javob",
      autoAnswered: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("call_logs").doc("c_secret").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("call_logs").get()
  );
});

test("Shadow field and invalid schema rejection: rejects ghost fields and invalid enum", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("voice_notes").doc("n_bad").set({
      userId: ALICE_UID,
      title: "Test",
      content: "Content",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid mode enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("m_bad").set({
      userId: ALICE_UID,
      prompt: "Test",
      mode: "INVALID_MODE",
      finalSpeech: "Speech",
      architectReport: "Arch",
      strategistPlan: "Strat",
      sentinelSecurityNote: "Sec",
      creativeSpark: "Spark",
      createdAt: now,
      updatedAt: now,
    })
  );
});
