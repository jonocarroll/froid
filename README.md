# froid

<img width="100" height="100" align="middle" src="froid.png"/>

Write Android apps in the **Frege** programming language (Haskell on the JVM) —
pure Frege, no Kotlin, with much less ceremony than the original thanks to
Frege's new [automated FFI](https://github.com/Frege/frege/pull/400).

froid is two things:

1. **A general Frege-on-Android library** — thin handles over `android.view` /
   `android.widget` / `android.app`, with the boilerplate erased by auto-FFI and
   the irreducible glue (the Activity, callbacks) hidden in the library. App code
   writes no Java and no Kotlin.
2. **A declarative UI in the Elm Architecture, backed by FRP** — built on the
   View system. You write `update :: Msg -> Model -> Model` and
   `view :: Signal Model -> Ui Msg`; froid keeps the views in sync.

## A counter, in full

```frege
module app.Counter where

import froid.app.Activity
import froid.ui.App
import froid.frp.Signal

native module type Activity where {}      -- the one line that makes this an Activity

data Msg = Increment

update :: Msg -> Int -> Int
update Increment n = n + 1

view :: Signal Int -> Ui Msg
view model = column
    [ dynText (fmap (\n -> "Count: " ++ show n) model)
    , button "Increment" Increment
    ]

counter :: App Int Msg
counter = App { initial = 0, update = update, view = view }

onCreate :: Activity -> IO ()
onCreate this = runApp this counter
```

No `findViewById`, no listeners by hand, no XML, no Kotlin. Buttons carry the
`Msg` they dispatch; state evolves in one place (`update`).

## The Elm Architecture (`froid.ui.App`)

- `App { initial, update, view }` — your whole program.
- `runApp :: Activity -> App model msg -> IO ()` — installs it.
- Widgets: `text` / `dynText` / `headline` / `button` / `primaryButton` /
  `flatButton` / `column` / `row` / `gap` / `flexSpacer`. `view` takes a
  `Signal model`, so only the labels whose text changed are redrawn.

## The FRP core (`froid.frp.Signal`)

A small, discrete, push-based FRP — the natural Frege idiom for reactive UIs:

- `Event a` — a stream of occurrences: `newEvent`, `fmap`, `never`, `mergeE`,
  `filterE`, `snapshot`/`tag`.
- `Signal a` — a value over time: `accumS` (Elm's `foldp`), `stepper`, `react`,
  `fmap`. The accumulator is strict (no `foldl` leak); the UI thread is
  single-threaded so cells need no locking.

The Elm `App` layer funnels every `Msg` through one event, so app authors rarely
touch these directly — they're there when you want raw reactive wiring.

## How an Activity works (no glue in app code)

A module becomes an Android Activity with one line, `native module type Activity
where {}` — its generated class extends `froid.app.FregeActivity` (plain Java in
the library), which bridges Android's `onCreate` to your
`onCreate :: Activity -> IO ()`. The manifest points at the module class
(e.g. `app.Counter`).

## Repository layout

```
froid/                 the library (pure Frege + a tiny Java Activity base)
  src/frege/froid/…      frp/Signal, ui/App, view, widget, content, app
  src/main/java/…        froid.app.FregeActivity (the only Java; precompiled)
froid-gradle-plugin/   the publishable Gradle plugin (id "io.github.mchav.froid"),
                       included as a build; wires Frege compilation into AGP
examples/
  counter/             minimal counter
  geoquiz/             GeoQuiz — true/false quiz with color-coded feedback
libs/                  vendored Frege compiler snapshot
```

The top level holds no Android config — `android { … }` lives only in the module
build files, not at the root.

### Using froid in your own app

```kotlin
plugins {
    id("com.android.application")
    id("io.github.mchav.froid")          // wires Frege compilation
}
dependencies { implementation("io.github.mchav:froid:…") }
// write your UI in src/frege/, point the manifest at your module class
```

## Building

### 1. Build the Frege compiler (one-time)

froid needs the auto-FFI compiler from Frege `master` (PR #400; no release has it
yet). The script pins a known-good commit and vendors the jar to `libs/`.

**Requirements:** a JDK in the **8–17** range (17 recommended), `make`, `curl`,
`jar`. `byacc` is not needed.

```bash
JAVA_HOME=/path/to/jdk17 tools/build-frege-snapshot.sh
```

### 2. Build and run a sample

```bash
./gradlew :geoquiz:assembleDebug
./gradlew :geoquiz:installDebug      # onto a running emulator/device
# or open in Android Studio and press Run
```

`compileFrege<Variant>` (from `froid-gradle-plugin`) runs the Frege compiler over
each module's `src/frege`, generating Java that AGP compiles and dexes.

## Status

**Builds and runs.** `:geoquiz` and `:counter` assemble to APKs and run on an
emulator (AGP 8.13, compileSdk 36): the question renders, True/False checks the
answer with color-coded feedback, and Prev/Next navigate — all driven by the
Frege FRP.

**Roadmap:** more widgets (EditText, images, lists with stable keys); the deeper
FRP correctness items (subscription disposal for rotation, transactional
glitch-freedom) are noted but not needed by the current static view tree; folding
more of the original View bindings back in where useful (they remain in git
history on `master`).

Frege `master` is pinned at commit `067cad057385d36134812bde3fc1b453e5274855`.

## License

Apache License 2.0. froid is © Michael Chavinda and contributors.
