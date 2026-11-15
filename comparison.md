# Four terminal UI stacks, one app

The same app was built four times: `jnz.FolderViewApp`, a full-screen, responsive, Midnight Commander-style folder
browser (default colors, no configuration; spec in `prompt/1.md`). Scala 3.9.0, sbt, JDK 25, macOS.
Numbers about GitHub were read with the GitHub API on 2026-11-15. Everything about our code comes from this repo.

| Branch | Library | Version used |
|---|---|---|
| `kyo` (= `master`) | [tui4s](https://github.com/yuri4n/tui4s) on [Kyo](https://getkyo.io) | `tui4s-*` 0.1.1-RC1 |
| `jatatui` | [jatatui](https://github.com/oyvindberg/jatatui), plus cats-effect 3.7.1 and fs2 3.14.0 | 0.31.0 |
| `termflow` | [TermFlow](https://github.com/llm4s/termflow) | 0.4.0 |
| `lanterna` | [Lanterna](https://github.com/mabe02/lanterna) | 3.1.5 |

All four now live in one sbt build (modules `core`, `lanterna`, `termflow`, `jatatui`, `tui4kyo`; see `README.md` for
`sbt "<module>/runMain jnz.<module>.FolderViewApp"`). The shared `core` holds the model and the screen text; the tui4s/Kyo
version was refactored onto it, and the `stty` code of Lanterna moved into `core` and serves TermFlow too. The sections
below describe each version as it was first written on its branch. One fact from the combined build: TermFlow also
needed its own terminal backend on `/dev/tty` to show anything under `sbt runMain` (JLine gets pipes and falls back to a
dumb terminal), and closing its key reader blocked until the next key.

## What was built, per stack

Common to `jatatui`, `termflow` and `lanterna`: the pure model `FolderModel.scala` (116 lines, identical in all three:
`Entry`, `State`, `Action`, `listing`, `apply(state, action, page)`, wide-character `cut`/`fit`, `line`) and the same
ScalaTest specs for it (143 lines). Only the key mapping, the drawing and the event loop differ. The `kyo` version came
first and has everything in one file with no tests; the model was extracted from it.

### tui4s on Kyo (`kyo`)

- Scala 3 library. A kyo-ui value (`div`, `span`) describes the screen; tui4s lays it out in terminal cells and paints it
  with jatatui. The core runs on Kyo (`A < Sync`, `A < Async`, `Signal`).
- State is a `SignalRef[State]`; keys arrive through `onKeyDown`; the screen is `state.combineLatest(size).render`.
  Resize is a signal (`Tui.size`), no code for it in the app.
- Build needs a forked JVM with `connectInput`, `--enable-native-access=ALL-UNNAMED` and four `-Dkyo.scheduler.*`
  properties ("the scheduler values of tui4s", `build.sbt` on `master`). A plain `sbt run` without them is not supported.
- Status of the library: release candidate (`0.1.1-RC1`), its README says Kyo and kyo-ui are at 1.0.0-RC7 and that ZIO and
  Cats Effect integrations are not in this version (issues #18, #19).
- Our code: 156 lines in one file, no tests. Model, key handling and drawing are mixed, so nothing could be unit-tested
  without the terminal runner (tui4s has a headless testkit, `tui4s-testkit`, not used by us).

### jatatui (`jatatui`)

- Java port of ratatui (Rust). The backend is JNI to the Rust crossterm crate, so native libraries are loaded and the
  forked JVM needs `--enable-native-access=ALL-UNNAMED`.
- jatatui itself has no event loop and no effect system. We chose cats-effect `IOApp` + `Ref` + an fs2 `Stream` of events
  (spec: `docs/superpowers/specs/2026-11-15-folderview-jatatui-design.md`). The terminal is a `Resource`
  (raw mode, alternate screen, restored on exit or crash).
- Events are pulled: `ct.poll(timeout)` and `ct.read()`. Java event classes are matched by type test
  (`case c: KeyCode.Char if c.c == 'k'`), which is verbose in Scala.
- Drawing uses jatatui widgets (`Block`, `List`, `Paragraph`) through `term.draw(frame => ...)`.
- Our code: 239 main lines (model 116, view 42, app 81), 238 test lines.

### TermFlow (`termflow`)

- Scala 3 library, Elm architecture: `TuiApp[Model, Msg]` with `init`, pure `update`, `view`, commands (`Cmd`) and
  subscriptions (`Sub`). Own TTY backend, key decoder and ANSI diff renderer; no cats-effect or ZIO.
- Keys come as `Sub.InputKey` → `Msg.Key`; resize as `Sub.TerminalResize(250, ...)`, which polls every 250 ms
  (the interval is a parameter we set, so a resize shows within about a quarter of a second).
- `toMsg` (prompt input) is abstract and must be implemented even in an app with no prompt; ours returns an error.
- Our code: 203 main lines (model 116, view 28, app 59), 235 test lines. The view builds `RootNode`/`TextNode` values, so
  the pure `FolderView.lines` is tested directly and only a thin `view` is TermFlow-specific.
- `sbt run` shows an empty screen (the forked JVM has no tty); the app is started from the staged script
  (`sbt stage`, `JavaAppPackaging`), as the README of this branch says.

### Lanterna (`lanterna`)

- Java library (first commit in 2010). We use only the `Screen` API (not the GUI widgets): draw into a back buffer,
  `refresh()` writes the difference.
- No effect system, no event model: a tail-recursive loop calls `pollInput()` (returns `null` when nothing is pressed) and
  sleeps 50 ms; resize comes from `Terminal.addResizeListener` (SIGWINCH) as a flag.
- Our code: 256 main lines (model 116, view 34, app 63, `SttyTerminal` 43), 294 test lines. `FolderView.draw` is tested
  on Lanterna's `DefaultVirtualTerminal` (bold cells, footer, tiny sizes).
- Needs no `--enable-native-access` (checked: the app ran and quit without the flag, no warning).

## How it was made

As reported by the author (not measurable from the repo):

- All four versions were written by Claude Code on a $20 per month subscription (plus VAT).
- The `jatatui`, `termflow` and `lanterna` versions were written by Sonnet models and used less than 50 % of one
  5-hour usage quota in total. This includes everything around the code: the specs, the plans, the debugging and the
  tests.
- The `kyo` (tui4s) version was written by Haiku.
- The code written by the models is the application; the libraries compared are third-party.

Metals MCP (the compiler, source and symbol tools of the Scala language server; `prompt/1.md` asks to use it) was used
in one session only, counted from the tool calls in the 17 Claude Code transcripts of this project on 2026-11-15:

- The Haiku session of the `kyo` version made 37 calls: `inspect` 14, `get-source` 6, `glob-search` 4,
  `typed-glob-search` 4, `list-modules` 3, `compile-full` 2, `find-dep` 2, `get-docs` 2.
- The other 16 transcripts, including all Sonnet sessions (`jatatui`, `termflow`, `lanterna`), made no Metals call.
  They compiled and tested with `sbt` and read library APIs with `javap`, `grep` and the library sources.

## Problems met

Problems 1–4 were met and fixed in this session. For the other three stacks only what the repo records is listed; their
first runs happened in earlier sessions and no log of the troubles was kept.

1. **Lanterna: no first screen, blocked at startup.** `TerminalScreen`'s constructor asks the terminal for its size with a
   cursor-position query (`ESC[6n`) and waits for the reply (`waitForCursorPositionReport`, found in a thread dump). In a
   pty that never answers the app printed 52 bytes and then nothing. Fix: `SttyTerminal` overrides `findTerminalSize`
   and reads `stty size < /dev/tty`. After it the first screen appears in 0.18 s with or without a reply.
2. **Lanterna: hanging, then constant blinking.** The first loop redrew every 20 ms and called `doResizeIfNecessary()` on
   every pass. Fix: redraw only after a state change or a resize, resize through a listener flag, poll every 50 ms.
   Not separately proven to be the cause of the blinking: after the fix the idle app wrote no bytes in a 6 s pty run,
   and the blinking was not reported again.
3. **Lanterna: `sbt run` showed an empty screen in iTerm2, the IntelliJ terminal and Terminal.app,** while my first
   pseudo-terminal test drew correctly. A thread dump showed the app was running its loop. Cause (inferred from the
   fix, not traced inside sbt): sbt pipes a forked process's stdin and stdout through its own terminal handling
   (`connectInput`, `StdoutOutput`), which in a real terminal swallowed the output. Fix: the app opens `/dev/tty` for
   input and output itself. Checked in Terminal.app by driving the window with AppleScript: first screen, `j`+Return,
   `q` → exit code 0. iTerm2 and IntelliJ were confirmed by the user ("now works").
4. **Lesson from 3:** my test harness ran with `TERM` unset, so sbt skipped its terminal features and the test hid the
   defect. A real terminal emulator was needed to reproduce it.
5. **jatatui: a blocking native `read` cannot be cancelled** (commit `428e9f1`, "Poll before read so cancel works"):
   the wait is split into `poll`s of 100 ms and `read` runs only when an event is ready. The same commit cleared a
   stale error message after a successful step.
6. **tui4s/Kyo:** the build carries scheduler properties (`-Dkyo.scheduler.cycleIntervalNs=10000000` and three more),
   native access and `connectInput`; the repo does not record why each is needed.
7. **TermFlow: `sbt run` has no tty,** so the screen stays empty; use the staged script. Resize is a 250 ms poll,
   not an event.
8. **All four:** terminal cells are not characters. CJK and emoji take two cells, so `cut`/`fit` in `FolderModel` count
   cells per code point (a table of wide ranges, not a full Unicode width implementation).

Not re-tested for this comparison: whether `sbt run` works in iTerm2 for the `kyo` and `jatatui` branches (the `jatatui`
README says `sbt run`; `termflow` and the older Lanterna attempts needed the changes above).

## Other things worth knowing

- **Most of the effort was terminal plumbing, not the library API:** tty ownership, size discovery, forked JVMs under
  sbt, resize. The pure model and its tests moved between three stacks unchanged; that split is why the
  `jatatui`, `termflow` and `lanterna` branches were cheap to write.
- **Testability follows the architecture.** `kyo` has no tests; the other three test the model and the pure
  `FolderView.lines`. Only Lanterna's virtual terminal and tui4s's testkit let a test see drawn cells; the jatatui spec
  said a rendering test would be written "only if a test backend exists" and none was written.
- **Cost of a second effect system:** `jatatui` brings cats-effect and fs2 only because we chose them; Lanterna and
  TermFlow need none. The Elm-style `termflow` gives the cleanest `update` function; Lanterna gives the least code to
  understand and the fewest dependencies (one jar for Lanterna 3.1.5).
- **Maturity:** tui4s and TermFlow are about a year old with 1–3 human contributors; jatatui is a one-person project
  with a long prehistory; Lanterna is the only one with a long record and many contributors. See the metrics below.
- **Licences:** tui4s Apache-2.0, jatatui MIT, TermFlow MIT, Lanterna LGPL-3.0 (matters if linking terms matter).
- **Supported platforms** were checked only on macOS here.

## Metrics, as of 2026-11-15

Months = days / 30.44. "Contributors" = entries of the GitHub contributors API with anonymous ones included.

| | tui4s (`kyo`) | jatatui | TermFlow | Lanterna |
|---|---|---|---|---|
| Language of the library | Scala 3 | Java (JNI to Rust crossterm) | Scala 3 | Java |
| Used from our code | Scala 3 | Scala 3 | Scala 3 | Scala 3 |
| Effect system | Kyo (`A < Sync`, `Signal`) | none in the library; we added cats-effect + fs2 | none; own `Cmd`/`Sub` | none; blocking loop |
| Architecture | signals and components (kyo-ui) | immediate-mode widgets, optional React-style layer | Elm (`update`/`view`) | back-buffer screen, no model |
| Events decoupled from the loop | yes (handlers, signals) | no, pulled with `poll`/`read` (we wrapped them in an fs2 `Stream`) | yes (`Sub` → `Msg`) | no, `pollInput()`; resize via listener |
| Resize handling | signal | `Event.Resize` | 250 ms poll (our interval) | SIGWINCH listener |
| Native access flag | in our build (via jatatui) | in our build (JNI) | in our build, not tested whether needed | not needed (tested) |
| Our version written by | Haiku | Sonnet | Sonnet | Sonnet |
| Our main / test lines | 156 / 0 | 239 / 238 | 203 / 235 | 256 / 294 |
| Version used | 0.1.1-RC1 | 0.31.0 | 0.4.0 | 3.1.5 |
| GitHub repo created | 2025-11-09 | 2022-12-04 | 2025-12-27 | 2015-06-29 |
| Months since repo created | 12.2 | 47.4 | 10.6 | 136.6 |
| Months since first commit of the main history | 12.2 | 6.4 (history restarted 2026-05-03; older code on `main-tui-scala`, `legacy-port`) | 10.6 | 192.2 (first commit 2010-11-08) |
| Contributors on GitHub | 2 (a human with 30 commits, the `claude` account with 174) | 1 | 4 entries = 3 people + 1 anonymous entry (likely the same person as `rorygraves`) | 75 (64 accounts, 11 anonymous) |
| Commits | 204 | 83 | 178 | 2561 |
| Stars | 4 | 289 | 1 | 2636 |
| Last push | 2026-11-11 | 2026-08-22 | 2026-06-15 | 2026-07-07 |
| Licence | Apache-2.0 | MIT | MIT | LGPL-3.0 |
