# Meshwar (D-RIDE)

Android ride-hailing app for Dayrout, Assiut, Egypt. The repository is named Meshwar and the app is called D-RIDE inside the code. Passengers and drivers use the app, and prices are in EGP with cash payment.

**Status: learning project, work in progress.** It was started with AI-assisted tooling (Google AI Studio / Gemini) and I am working through it to understand how it is built. Expect rough edges and unfinished parts.

## Stack
- Kotlin, Jetpack Compose, Material 3
- Firebase: Cloud Firestore and Google Sign-In
- Gradle (Kotlin DSL), minSdk 24, targetSdk 36

## What is in the repo
- `app/src/main/java/com/example/`: code split into `data` (repositories, models, location), `domain` (ride status, roles, vehicle categories, fare estimate) and `presentation` (passenger, driver, admin, auth screens)
- `DATABASE_SCHEMA.md`: Firestore collections and the fields each one allows
- `firestore.rules` and `firestore.test.js`: Firestore security rules and their tests
- `app/src/test/`: unit tests, including `DRideDomainTest.kt`

## Security rules (access control)
The Firestore rules start from default-deny and then allow only specific reads and writes. They validate field types and sizes, restrict who can read user data (the owner or an admin), and check ride status changes against an allowed list of transitions. `firestore.test.js` tests the rules with the Firebase emulator, for example that unauthenticated users cannot read or write, and that a user cannot give themselves a higher role.

## How to run
1. Open the project in Android Studio.
2. Connect it to your own Firebase project with Cloud Firestore and Google Sign-In enabled, and update the Firebase config in `app/src/main/res/values/firebase_applet_config.xml`.
3. Optional: copy `.env.example` to `.env` if you want to use a Gemini API key. Never commit a real key.
4. Build and run on an emulator or device.

To test the Firestore rules: install the Firebase CLI, start the Firestore emulator (`firebase.json` uses port 8085), then run `node --test firestore.test.js` after installing `@firebase/rules-unit-testing`.

## Not done yet
This README was written from reading the code and docs. I have not verified every screen or feature end to end.
