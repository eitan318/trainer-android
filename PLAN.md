# Workout App — Implementation Plan

## Stack
- Kotlin + Jetpack Compose (Material 3), single `:app` module
- Build/run from CLI: `./gradlew installDebug` + `adb` (no Android Studio)
- State: one repository exposing `StateFlow<AppData>`; ViewModel per screen
- Persistence: whole app state as one kotlinx.serialization JSON file in `filesDir`,
  loaded at startup, rewritten on change
- Export/import: SAF file picker (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`)
  copying that same JSON
- Timers: coroutines in the flow ViewModel; `FLAG_KEEP_SCREEN_ON` during workout

## Data model
Overridable props are nullable at each level. One `resolve()` function walks
**exercise > section > workout > global defaults** (first non-null wins). Unit-tested.

```
Defaults  — restBetweenSections, restBetweenExercises, weight, sets, reps,
            repsOrDuration, duration, setRest, volumeIncrementPercent
Exercise  — id, name, description?, weight?, reps?/duration?, sets?, setRest?,
            restAfter?, volumeAtReset, resetDate        (lives in the archive)
Workout   — id, name, restBetweenSections?, sections
Section   — id, name, restBetweenExercises?, restAfter?, executionCount, exerciseIds
```

Workouts reference archived exercises by id (shared: add existing archived
exercise to workout, move between sections).

### Volume indicator (workout view only, not flow)
Volume = (reps or duration) × sets × weight. Compared to the snapshot stored at
the last volume reset (button per exercise):
- **Green**: grew ≥ volume-increment-percent since reset
- **Yellow**: grew, but less than that
- **Red**: no growth

## Milestones (one at a time, reviewed before moving on)
0. [ ] `git init`
1. [ ] Scaffold — Gradle files, manifest, empty Compose activity; builds + installs on device
2. [ ] Data layer — models, JSON persistence, `resolve()` + unit tests
3. [ ] Settings screen — edit global defaults
4. [ ] Exercise archive — CRUD
5. [ ] Workouts — list + editor: sections CRUD, add/remove archived exercises,
       move between sections, per-level overrides
6. [ ] Volume indicator — color dot per exercise in workout view + reset button
7. [ ] Workout flow — fullscreen, "set 1/3" header, exercise view with description
       toggle + start-rest button, big rest timer with next set/exercise preview,
       "workout finished" screen
8. [ ] Export / import — SAF picker, JSON of all workouts + archive
