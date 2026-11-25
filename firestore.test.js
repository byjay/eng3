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

function validFamilyStreak(uid, kakaoUserId, displayName, ts, overrides = {}) {
  return {
    userId: uid,
    kakaoUserId,
    displayName,
    avatarUrl: "https://k.kakaocdn.net/dn/sample/avatar.jpg",
    streakCount: 14,
    todayWordsCompleted: 25,
    totalXp: 1200,
    lastStudyDate: "2026-11-24",
    shareWithFriends: true,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

test("FamilyStreaks denies unauthenticated access and collection-wide list queries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("familyStreaks").doc("kakao_1001").get());
  await assertFails(
    unauthDb
      .collection("familyStreaks")
      .doc("kakao_1001")
      .set(validFamilyStreak(ALICE_UID, "kakao_1001", "아빠", now))
  );
  await assertFails(aliceDb.collection("familyStreaks").get());
});

test("Connected Kakao Friends can read opt-in streaks, cannot read opt-out streaks, and strangers have zero existence oracle", async () => {
  const CHARLIE_UID = "charlie_789";
  const created = new Date(Date.now() - 5000);

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Alice links kakao_1001 and has Bob (kakao_2002) and unjoined friend (kakao_9999) in connectedKakaoIds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID, created),
      kakaoUserId: "kakao_1001",
      connectedKakaoIds: ["kakao_2002", "kakao_9999"],
    })
  );

  // Bob links kakao_2002 and creates his opt-in familyStreak entry
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validProfile(BOB_UID, created),
      kakaoUserId: "kakao_2002",
      connectedKakaoIds: ["kakao_1001"],
    })
  );
  await assertSucceeds(
    bobDb
      .collection("familyStreaks")
      .doc("kakao_2002")
      .set(validFamilyStreak(BOB_UID, "kakao_2002", "원삐", created, { streakCount: 21 }))
  );

  // Alice creates her own kakao_1001 and uid_alice_123 entries
  await assertSucceeds(
    aliceDb
      .collection("familyStreaks")
      .doc("kakao_1001")
      .set(validFamilyStreak(ALICE_UID, "kakao_1001", "아빠 국왕", created, { streakCount: 15 }))
  );
  await assertSucceeds(
    aliceDb
      .collection("familyStreaks")
      .doc(`uid_${ALICE_UID}`)
      .set(validFamilyStreak(ALICE_UID, "", "아빠 국왕", created, { streakCount: 15 }))
  );

  // Alice (connected friend) CAN read Bob's opt-in streak
  const bobSnap = await assertSucceeds(aliceDb.collection("familyStreaks").doc("kakao_2002").get());
  if (!bobSnap.exists || bobSnap.data().streakCount !== 21) {
    throw new Error("Expected Alice to read Bob's opt-in family streak");
  }

  // Alice CAN cleanly get a connected Kakao friend who hasn't joined yet (exists === false)
  const unjoinedSnap = await assertSucceeds(
    aliceDb.collection("familyStreaks").doc("kakao_9999").get()
  );
  if (unjoinedSnap.exists) {
    throw new Error("Expected unjoined friend doc not to exist");
  }

  // Charlie (stranger NOT connected to Bob) is DENIED on BOTH existing and non-existing docs (zero existence oracle)
  await assertSucceeds(charlieDb.collection("users").doc(CHARLIE_UID).set(validProfile(CHARLIE_UID, created)));
  await assertFails(charlieDb.collection("familyStreaks").doc("kakao_2002").get());
  await assertFails(charlieDb.collection("familyStreaks").doc("kakao_nonexistent").get());
  await assertFails(charlieDb.collection("familyStreaks").doc(`uid_${ALICE_UID}`).get());

  // Bob opts out of sharing -> Alice is now DENIED reading Bob's streak
  await assertSucceeds(
    bobDb.collection("familyStreaks").doc("kakao_2002").update({
      shareWithFriends: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("familyStreaks").doc("kakao_2002").get());

  // Owner Bob can still read & update his own opted-out streak
  await assertSucceeds(bobDb.collection("familyStreaks").doc("kakao_2002").get());
});

test("FamilyStreaks blocks hijacking, cross-user writes, ghost fields, @DocumentId id injection, and invalid values", async () => {
  const created = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID, created),
      kakaoUserId: "kakao_1001",
      connectedKakaoIds: ["kakao_2002"],
    })
  );

  // Alice cannot create an entry for a kakao ID not linked to her own /users doc
  await assertFails(
    aliceDb
      .collection("familyStreaks")
      .doc("kakao_2002")
      .set(validFamilyStreak(ALICE_UID, "kakao_2002", "Spoofer", created))
  );

  // Alice cannot inject @DocumentId 'id' or ghost field or http:// avatarUrl or negative streak
  await assertFails(
    aliceDb
      .collection("familyStreaks")
      .doc("kakao_1001")
      .set({ ...validFamilyStreak(ALICE_UID, "kakao_1001", "아빠", created), id: "kakao_1001" })
  );
  await assertFails(
    aliceDb
      .collection("familyStreaks")
      .doc("kakao_1001")
      .set(validFamilyStreak(ALICE_UID, "kakao_1001", "아빠", created, { avatarUrl: "http://insecure.example/a.png" }))
  );
  await assertFails(
    aliceDb
      .collection("familyStreaks")
      .doc("kakao_1001")
      .set(validFamilyStreak(ALICE_UID, "kakao_1001", "아빠", created, { streakCount: -1 }))
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb
      .collection("familyStreaks")
      .doc("kakao_1001")
      .set(validFamilyStreak(ALICE_UID, "kakao_1001", "아빠", created))
  );

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("familyStreaks").doc("kakao_1001").update({
      streakCount: 16,
      todayWordsCompleted: 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt or userId mutation fails
  await assertFails(
    aliceDb.collection("familyStreaks").doc("kakao_1001").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot update or delete Alice's streak
  await assertFails(
    bobDb.collection("familyStreaks").doc("kakao_1001").update({
      streakCount: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("familyStreaks").doc("kakao_1001").delete());

  // Owner Alice can delete her own entry
  await assertSucceeds(aliceDb.collection("familyStreaks").doc("kakao_1001").delete());
});
