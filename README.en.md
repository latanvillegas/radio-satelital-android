# Radio Satelital Android

[Version en espanol](README.md)

[![Android](https://img.shields.io/badge/Android-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-0095D5?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.4-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%26%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)

Native Android app (Kotlin + Jetpack Compose) to listen to radio stations, browse by country, and manage community-submitted stations.

## App type

Radio Satelital is an internet radio application.
It lets users play live stations, browse radios by location, and manage user-submitted stations.

## App version

- Current version: 1.0
- Version code: 1

## Technical stack

- Kotlin
- Jetpack Compose + Navigation
- Media3 ExoPlayer
- Firebase Authentication
- Firebase Firestore

## Requirements

- Android Studio (recent version)
- JDK 17
- Android SDK 34
- Gradle Wrapper included in this repository

## Firebase setup

1. Create (or open) a project in Firebase Console.
2. Register an Android app with this package name: `com.app.radiosatelital`.
3. Download `google-services.json`.
4. Copy the file to `app/google-services.json`.
5. In Firebase Console, enable:
   - Authentication (Anonymous and/or Email/Password)
   - Firestore Database

Important:
- `google-services.json` should not be committed to a public repository.
- Admin email must be configured as a local Gradle property, not in source code.

### Local admin email setup

To keep admin access from Settings without exposing your email in Git, define `ADMIN_EMAIL` in a non-versioned local file.

Recommended option (global on your machine):

File: `~/.gradle/gradle.properties`

```properties
ADMIN_EMAIL=your-admin-email@domain.com
```

Project option (also non-versioned):

File: `local.properties`

```properties
ADMIN_EMAIL=your-admin-email@domain.com
```

## Build and run

From the project root:

```bash
./gradlew :app:assembleDebug
```

To install on a connected device/emulator:

```bash
./gradlew :app:installDebug
```

You can also open the project in Android Studio and run the `app` configuration.

## Main structure

- `app/src/main/java/com/app/radiosatelital/ui`: screens and UI state
- `app/src/main/java/com/app/radiosatelital/data`: data access (Firebase, repositories, artwork)
- `app/src/main/java/com/app/radiosatelital`: main activity, playback service, and base models

## Screenshots

Add real app screenshots here to show the current UI state.

Suggested list:

- Home / Explorer
- Search screen
- Player screen
- Moderation screen

Example block:

```md
![Home](URL_OR_IMAGE_PATH)
![Search](URL_OR_IMAGE_PATH)
![Player](URL_OR_IMAGE_PATH)
![Moderation](URL_OR_IMAGE_PATH)
```

## Architecture and data flow

Quick summary:

- Compose UI in the presentation layer.
- ViewModels expose state and screen actions.
- Repositories and data sources handle Firebase and other data providers.
- Playback service keeps audio running in the background.

General flow:

1. UI triggers user actions (search, submit station, play).
2. ViewModel processes the action and queries a repository.
3. Repository delegates to Firebase or another data source.
4. Result goes back to the ViewModel and updates UI state.
5. For audio playback, the service keeps playback independent from screen lifecycle.

## Troubleshooting

- Google Services error:
  - Verify `app/google-services.json` exists and package name matches `com.app.radiosatelital`.
- Authentication/Firestore error:
  - Confirm Authentication and Firestore are enabled in Firebase Console.
- Java build error:
  - Confirm the project is using JDK 17.

## How to contribute

1. Open an issue to report bugs or propose improvements.
2. Fork the repository and create a descriptive branch.
3. Implement your change with clear, small commits.
4. Verify the app builds with `./gradlew :app:assembleDebug`.
5. Open a Pull Request with scope, changes, and evidence.

## Rights reserved

This project uses a proprietary license with all rights reserved.
See terms in `LICENSE`.

## Support the creator

If you want to support the project creator, you can:

- Share the app and repository.
- Report bugs and suggest useful improvements.
- Contribute code, tests, or documentation.
- Open an issue to coordinate direct support or sponsorship options.
