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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validProfile(uid, ts) {
  return {
    userId: uid,
    displayName: "Minjun",
    selectedGrade: 2,
    selectedTeacherId: "emma_uk",
    dailyGoal: 30,
    streakCount: 3,
    totalXp: 450,
    coins: 220,
    hearts: 5,
    todayWordsCompleted: 12,
    lastStudyDate: "2026-11-22",
    unlockedItems: ["badge_starter"],
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, now)));
  await assertFails(db.collection("users").doc(ALICE_UID).collection("wordMastery").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("scenarioProgress").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("sessions").get());
});

test("Owner can create and partially update UserProgress, while ghost fields and immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, created))
  );

  // Partial update of mutable fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 350,
      totalXp: 600,
      todayWordsCompleted: 30,
      unlockedItems: ["badge_starter", "item_golden_mic"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field injection fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 500,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user access to profile and subcollections is strictly denied", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set(validProfile(BOB_UID, created));
    await db.collection("users").doc(BOB_UID).collection("wordMastery").doc("g1_001").set({
      wordId: "g1_001",
      grade: 1,
      correctCount: 3,
      missCount: 1,
      speakingBestScore: 92,
      mastered: true,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ coins: 0, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("wordMastery").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("wordMastery").doc("g1_001").get());
});

test("Owner can manage wordMastery, scenarioProgress, and append-only sessions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // WordMastery create, update, list
  const wordRef = aliceDb.collection("users").doc(ALICE_UID).collection("wordMastery").doc("g2_015");
  await assertSucceeds(
    wordRef.set({
      wordId: "g2_015",
      grade: 2,
      correctCount: 1,
      missCount: 0,
      speakingBestScore: 88,
      mastered: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    wordRef.update({
      correctCount: 2,
      speakingBestScore: 96,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("wordMastery").get());

  // Reject extra @DocumentId 'id' field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wordMastery").doc("g2_016").set({
      id: "g2_016",
      wordId: "g2_016",
      grade: 2,
      correctCount: 1,
      missCount: 0,
      speakingBestScore: 80,
      mastered: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // ScenarioProgress create, update, list
  const scenRef = aliceDb.collection("users").doc(ALICE_UID).collection("scenarioProgress").doc("sc_001");
  await assertSucceeds(
    scenRef.set({
      scenarioId: "sc_001",
      zoneId: 1,
      completedSentencesCount: 15,
      twistCleared: false,
      bestFluencyScore: 84,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    scenRef.update({
      completedSentencesCount: 30,
      twistCleared: true,
      bestFluencyScore: 95,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("scenarioProgress").get());

  // StudySessionLog create & list succeed; update is immutable (denied)
  const sessRef = aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_001");
  await assertSucceeds(
    sessRef.set({
      mode: "SPARTA_VOCAB",
      teacherId: "emma_uk",
      accuracyPercent: 93,
      wpm: 128,
      xpEarned: 150,
      coinsEarned: 60,
      createdAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(sessRef.update({ xpEarned: 9999 }));
});
