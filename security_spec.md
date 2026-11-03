# Astrafolio Security Specification (Zero-Trust Firestore Rules)

## 1. Data Invariants
1. **Authentication & Ownership Invariant**: Every document under `/users/{userId}/missions/{missionId}`, `/users/{userId}/call_logs/{callId}`, and `/users/{userId}/voice_notes/{noteId}` belongs strictly to `userId` where `request.auth != null && request.auth.uid == userId` and `incoming().userId == userId`.
2. **Path Variable Hardening (`isValidId`)**: All path IDs (`userId`, `missionId`, `callId`, `noteId`) must match `^[a-zA-Z0-9_\-]+$` and be at most 128 characters on `get`, `create`, `update`, and `delete`.
3. **Strict Key Allowlisting & Schema Bounds**:
   - `missions`: Required keys `['userId', 'prompt', 'mode', 'finalSpeech', 'architectReport', 'strategistPlan', 'sentinelSecurityNote', 'creativeSpark', 'createdAt', 'updatedAt']`, optional `['executedDeviceAction']`.
   - `call_logs`: Required keys `['userId', 'callerNumber', 'callerLabel', 'statusPreset', 'aiGeneratedReply', 'autoAnswered', 'createdAt', 'updatedAt']`.
   - `voice_notes`: Required keys `['userId', 'title', 'content', 'createdAt', 'updatedAt']`.
4. **Temporal & Immutability Invariant**: `createdAt` and `updatedAt` must be Firestore `timestamp` (`<= request.time`). On `update`, `userId` and `createdAt` are strictly immutable (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/Write**: `{ "userId": "anon", "prompt": "hack" }` with `request.auth == null` -> Rejected by `isSignedIn()`.
2. **Cross-User Read/Write**: Authenticated as `alice_123` reading `/users/bob_456/missions/m1` -> Rejected by `request.auth.uid == userId`.
3. **Identity Spoofing in Body**: Authenticated as `alice_123` writing to `/users/alice_123/missions/m1` with `{ "userId": "bob_456", ... }` -> Rejected by `incoming().userId == request.auth.uid`.
4. **Shadow Field Injection ("Ghost Field")**: Adding `"isAdmin": true` to a mission or call log payload -> Rejected by `incoming().keys().hasOnly(...)`.
5. **Immutable Field Mutation (`createdAt` / `userId`)**: Updating `createdAt` or `userId` on an existing document -> Rejected by immutability checks and `affectedKeys().hasOnly(...)`.
6. **ID Poisoning Attack**: Creating a document with a 500-char ID or special characters -> Rejected by `isValidId()`.
7. **Denial of Wallet (1MB String Payload)**: Writing a 50,000-character string into `prompt` or `aiGeneratedReply` -> Rejected by `.size() <= MAX` bounds in `isValidMission()` / `isValidCallLog()`.
8. **Enum Bypass on Mission Mode**: Setting `mode: "ROOT_SHELL"` -> Rejected by `data.mode in ['VOICE_COUNCIL', 'AGENT_DEBATE', 'TACTICAL_PROTOCOL']`.
9. **Type Confusion Attack**: Passing `autoAnswered: "true"` (string instead of bool) or `createdAt: 1700000000` (int instead of timestamp) -> Rejected by strict type checks.
10. **Future Timestamp Spoofing**: Setting `createdAt` to year 2099 -> Rejected by `data.createdAt <= request.time`.
11. **Required Field Deletion via Update**: Calling `FieldValue.delete()` on `prompt` or `aiGeneratedReply` during update -> Rejected because `isValid[Entity](incoming())` runs on every update.
12. **Blanket Collection Scraping**: Listing `/users/bob_456/call_logs` as `alice_123` -> Rejected by `allow list: if isOwner(userId) && resource.data.userId == request.auth.uid`.
