# Tablet Photo Frame (menu-feature) — Architecture

Author: Luann Campos (satuart) — recorded by Claude Code
Created: 08-25 08-08-2026
Last updated: 08-25 08-08-2026

> This worktree checks out the `menu-feature` branch of `Tablet-Photo-Frame`. The main
> worktree keeps `CLAUDE.md` (work tracking/objectives) and `GEMINI.md` (tech stack) split;
> this file exists only here because it doesn't have those yet. Fold this into `GEMINI.md`
> when the branch merges back, to keep one architecture doc per repo.

## Hard constraint

`minSdk = 19` (Android 4.4 KitKat) is non-negotiable — this runs on old/idle tablets
repurposed as photo frames. Every dependency added below is chosen because it doesn't
raise the effective minSdk and has no/negligible runtime cost:

- `androidx.lifecycle:lifecycle-viewmodel-ktx` / `lifecycle-runtime-ktx` — pure Kotlin,
  supports API 14+.
- ViewBinding — compile-time codegen, zero runtime dependency.
- `kotlinx-coroutines` — already in use.

No DI framework (Hilt/Koin). At 2 screens and 3-4 collaborators, annotation-processing
overhead and the extra APK size aren't worth it — manual constructor injection via a small
`ViewModelProvider.Factory` is enough and stays inspectable.

## Current shape (before rework)

- `MainActivity` does everything: `findViewById`, owns a raw `CoroutineScope`, holds
  `photosFiles` / `isPaused` as plain fields, runs the slideshow loop directly.
- State above is lost on rotation/process death (no `ViewModel`).
- `LoadPhotosUseCases` is called straight from the Activity — no seam to fake it in tests.
- `MenuTriggerController` already separates gesture/animation handling from the Activity
  via constructor callbacks (`onOpenSettings`, `onTogglePause`, `onRefresh`,
  `isPausedProvider`) — this shape is good and stays; only what's on the other end of
  those callbacks changes.
- `HoldGestureMath` is a pure object with a unit test — keep following this pattern for
  new logic (extract pure functions, test without Android framework classes).

## Target architecture: lightweight MVVM, XML views unchanged

```
View (Activity/XML/custom View)
   ↕ observes StateFlow / calls methods
ViewModel (androidx.lifecycle, viewModelScope)
   ↕ suspend calls
Repository (interface + impl)
   ↕
Data source (filesystem scan today; swappable later, e.g. SAF for scoped storage)
```

XML layouts and the View system stay as-is — this is an internal-structure change, not a
UI framework migration. No Compose.

### Layers

1. **View layer** — `MainActivity`, `SettingsActivity`, `HoldRingView`, `MenuTriggerController`.
   Passive: renders whatever state the ViewModel exposes, forwards user input as method
   calls. Use ViewBinding (`ActivityMainBinding` / `ActivitySettingsBinding`) instead of
   `findViewById`.
2. **ViewModel layer** — new `SlideshowViewModel`, scoped to `MainActivity` via
   `viewModels { SlideshowViewModelFactory(...) }`. Owns:
   - `photos: List<File>`
   - `currentPhoto: File?`
   - `isPaused: Boolean`
   Exposed as a single `StateFlow<SlideshowUiState>`. Runs the shuffle/advance loop in
   `viewModelScope`, survives rotation. `MainActivity` collects it in `onCreate` via
   `repeatOnLifecycle(STARTED)`.
3. **Repository layer** — `PhotoRepository` interface wrapping the existing
   `LoadPhotosUseCases` scan (`getSdCardImages(): List<File>`, called on `Dispatchers.IO`).
   Gives the ViewModel a fakeable seam for unit tests and a single place to extend photo
   sourcing later (e.g. SAF-picked folder) without touching ViewModel/View code.
4. **No DI container** — a 3-line factory (`SlideshowViewModelFactory(repository)`)
   constructed in `MainActivity.onCreate` is the entire wiring.

### What doesn't change

- `MenuTriggerController`'s gesture/animation/dim logic — it's View-layer concern, not
  business state, and its callback-based shape already matches the target (callbacks will
  just call `viewModel.togglePause()` / `viewModel.refresh()` instead of Activity fields).
- `SettingsActivity` — currently no business logic to extract; revisit if it grows beyond
  the toolbar back button.
- `HoldGestureMath` — already a pure, tested unit; keep new logic extracted the same way.

## Migration order (incremental, each step keeps the app buildable/runnable)

1. Enable `viewBinding = true`; convert `MainActivity` and `SettingsActivity` off
   `findViewById`.
2. Introduce `PhotoRepository` wrapping `LoadPhotosUseCases` (no behavior change).
3. Add `SlideshowViewModel` + `SlideshowViewModelFactory`; move photo list / `isPaused` /
   the slideshow loop out of `MainActivity` into it.
4. Rewire `MainActivity` to collect `StateFlow` and rewire `MenuTriggerController`'s
   callbacks to the ViewModel.
5. Add a unit test for `SlideshowViewModel` using a fake `PhotoRepository` (no Android
   framework dependency needed — `viewModelScope` is testable with
   `kotlinx-coroutines-test`).

## Explicitly out of scope for this rework

- Jetpack Compose — XML stays.
- Hilt/Koin — manual injection is sufficient at this size.
- Clean Architecture use-case layer — `PhotoRepository` is the only seam needed; adding a
  domain/use-case layer on top of one repository method would be ceremony without payoff.
- Scoped-storage migration (SAF) — real need given `minSdk 19` still supports legacy
  external storage, but the `PhotoRepository` seam is deliberately placed so this can land
  later without a ViewModel/View rewrite.
