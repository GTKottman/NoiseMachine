# NoiseMachine

Offline Android sleep-noise app focused on reliable overnight playback.

## MVP Included

- Play screen with large play and pause control
- Preset carousel with favorites
- Mixer with up to 5 procedural layers
- Timer presets: Nap, Sleep, Deep Sleep
- Library view for quick preset access
- Sleep Mode fullscreen dim view
- Offline Room storage plus DataStore settings
- Foreground playback service with audio focus handling

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Room + DataStore
- Foreground Service for background audio

## Notes

- The procedural audio engine is implemented as a real-time generated PCM stream.
- Presets are seeded on first launch.
- This repository currently does not include a Gradle wrapper binary, so CI and local build setup should add wrapper files or use an existing Android environment with Gradle tooling.
