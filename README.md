# Nova — journal & notes

Android app (Kotlin + Jetpack Compose). Notes, journal entries and photos (camera or gallery), in a bold card UI:
green backdrop, rounded white / yellow / orange cards, chunky wordmark, uppercase grotesque headlines with italic accents,
black pill buttons, bouncing spring animations and haptic feedback on every tap.

## Fonts (optional but recommended)
Put these in `app/src/main/assets/fonts/`:
- `caprasimo.ttf` – Google Fonts "Caprasimo" (Cooper-style wordmark)
- `intertight.ttf` – Google Fonts "Inter Tight" (Helvetica-style headlines/body)

Without them the app uses system fonts.

## Build
- GitHub: push to `main` → Actions → **Build APK** → download the `Nova-apk` artifact.
- Release: `git tag v1.0.0 && git push --tags` attaches `Nova.apk` to a GitHub Release.
- Local: open in Android Studio and Run, or `gradle assembleDebug`.

Camera uses the system camera app (no CAMERA permission needed). Data is stored on-device only.
