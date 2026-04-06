# Radio Satelital Android

[Versión en español](README.md)

[![Android](https://img.shields.io/badge/Android-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-0095D5?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.4-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%26%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)

Native Android app (Kotlin + Jetpack Compose) to listen to radio stations, browse by country, and manage community-submitted stations.

## Value proposition

Radio Satelital helps users quickly discover real stations by country, region, and city, with smooth playback and a clean experience from first launch.

## Project status

- Current status: Active (v1.0)
- Phase: Early production with continuous improvements
- Platform: Android

## Main features

- Live radio playback with background service support.
- Discovery by country, region, city, and continent.
- Fast station search.
- Dedicated player screen with clear controls.
- Community station submission flow.
- Moderation tools for submitted stations in admin mode.
- Theme and visual layout settings inside the app.

## Features matrix

| Feature | Available | Notes |
|---|---|---|
| Live playback | Yes | Powered by Media3 ExoPlayer |
| Station search | Yes | Quick filtering by name |
| Discovery by location | Yes | Country, region, city, continent |
| Community station submissions | Yes | Pending moderation workflow |
| Admin moderation | Yes | Approve or reject submissions |
| Theme and layout settings | Yes | Configurable from Settings |
| Synced favorites | No (roadmap) | Planned for upcoming iteration |

## Use cases

- Listen to local stations from a specific city.
- Discover new stations by country or continent.
- Submit a missing station for moderation.
- Manage community catalog updates in admin mode.

## App version

- Current version: 1.0
- Version code: 1

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

Recommendation: replace placeholders with 3 to 5 real screenshots before public release.

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

## Roadmap

- Favorites and personal collections.
- Recent listening history.
- Improved streaming metadata handling.
- Basic playback telemetry (opt-in).
- Better bulk moderation UX.

## For developers

### Technical stack

- Kotlin
- Jetpack Compose + Navigation
- Media3 ExoPlayer
- Firebase Authentication
- Firebase Firestore

### Requirements

- Android Studio (recent version)
- JDK 17
- Android SDK 34
- Gradle Wrapper included in this repository

### Minimal setup

1. Configure Firebase for Android app `com.app.radiosatelital`.
2. Place `google-services.json` in `app/google-services.json`.
3. Define `ADMIN_EMAIL` in a local Gradle property (`~/.gradle/gradle.properties` or `local.properties`).

### Quick build

```bash
./gradlew :app:assembleDebug
```

### Main structure

- `app/src/main/java/com/app/radiosatelital/ui`: screens and UI state.
- `app/src/main/java/com/app/radiosatelital/data`: data access (Firebase, repositories, artwork).
- `app/src/main/java/com/app/radiosatelital`: main activity, playback service, and base models.

## Troubleshooting

- Google Services error:
  - Verify `app/google-services.json` exists and package name matches `com.app.radiosatelital`.
- Authentication/Firestore error:
  - Confirm Authentication and Firestore are enabled in Firebase Console.
- Java build error:
  - Confirm the project is using JDK 17.

## FAQ

1. I cannot sign in as admin. What should I check first?
Make sure `ADMIN_EMAIL` is defined locally and the user exists in Firebase Authentication with Email/Password enabled.

2. Can the app be used without an account?
Yes. The app supports anonymous authentication for public flows.

3. How do I recover admin access if I forgot the password?
Use reset password from the app or from Firebase Authentication console.

4. What if a station does not play?
Check connectivity, stream availability, and provider-supported format.

## Security

- Do not commit `google-services.json`.
- Do not hardcode emails, passwords, or API keys in source code.
- Use local properties for sensitive settings (`ADMIN_EMAIL`).
- Rotate Firebase credentials if there was previous exposure.

## How to contribute

1. Open an issue to report bugs or propose improvements.
2. Fork the repository and create a descriptive branch.
3. Implement your change with clear, small commits.
4. Verify the app builds with `./gradlew :app:assembleDebug`.
5. Open a Pull Request with scope, changes, and evidence.

Recommended PR checklist:

- [ ] Debug build passes.
- [ ] Scope and risk clearly described.
- [ ] Screenshots included for UI changes.
- [ ] No secrets or credentials in changes.

## Rights reserved

This project uses a proprietary license with all rights reserved.
See terms in `LICENSE`.

## Support and contact

- Main support channel: repository Issues.
- For collaboration or sponsorship support: open an issue with contact context.

## Support the creator

If you want to support the project creator, you can:

- Share the app and repository.
- Report bugs and suggest useful improvements.
- Contribute code, tests, or documentation.
- Open an issue to coordinate direct support or sponsorship options.
