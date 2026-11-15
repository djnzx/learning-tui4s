# FolderViewApp on Lanterna – design

## Intent

The `lanterna` branch starts from the TermFlow version (`2acefe6`). The goal is to replace TermFlow with
[Lanterna](https://github.com/mabe02/lanterna) (Java terminal library) and keep the app the same: a full-screen,
responsive Midnight Commander-style folder browser, default colors, no configuration, no extra features
(`prompt/1.md`). Why: the same app has been built on kyo/tui4s, jatatui and TermFlow; Lanterna is the next library
to compare.

## Goals / non-goals

- Goals: same behavior and keys as today; `FolderModel` and `FolderView.lines` unchanged; tests stay green.
- Non-goals: Lanterna GUI widgets, colors, mouse, new keys, an Elm `Msg`/`update` layer.
- Anti-goal: rewriting the pure model to fit Lanterna.

## Approaches (picked: 1)

1. **Screen API, keep the model** (picked). `TerminalScreen` draws `FolderView.lines`; a small loop reads `KeyStroke`.
   Least change, model stays tested.
2. Lanterna GUI toolkit (`ActionListBox`). Lanterna owns selection and layout; the model becomes dead code.
3. Screen API plus an Elm loop. More structure than a 59-line app needs.

## Design

- `build.sbt`: drop `org.llm4s %% termflow`, add `com.googlecode.lanterna % lanterna % 3.1.3` (Java artifact, single `%`).
  `sbt run` still has no tty, so the staged-script run in the README stays.
- `FolderViewApp` (plain `object` with `main`, no framework trait):
  - `toAction(key: KeyStroke): Option[Action]`, same mapping as now: ArrowUp/`k`, ArrowDown/`j`, PageUp, PageDown,
    Home, End, Enter/ArrowRight → Open, Backspace/ArrowLeft → Parent, F10/`q`/Ctrl+C → Quit.
  - `main`: `DefaultTerminalFactory().createScreen()`, `startScreen()`, loop until Quit, `stopScreen()` in `finally`.
    Each pass: `doResizeIfNecessary()`, draw, then `pollInput()` with a short sleep (about 20 ms), so a resize
    redraws without a key press. The loop holds the `State` as a local `var` or a tail-recursive argument.
  - State change: `FolderModel(state, action, FolderModel.rows(height))` as today.
- `FolderView`: `lines` stays. `view(...)` (TermFlow `RootNode`) is replaced by `draw(screen, state)`: `clear`, then one
  `putString` per line with `SGR.BOLD` for the header and the selected entry, then `refresh`.
- Tests: `FolderModelSpec`, `FolderViewSpec` `lines` tests unchanged; the `view` test is replaced by a `draw` test on
  Lanterna's `DefaultVirtualTerminal` if that is cheap, otherwise dropped (builder's call); `KeysSpec` moves to `KeyStroke`.
- README: update the intro (TermFlow → Lanterna) and the branch list.

## Left to the builder

Exact Lanterna version check on Maven Central; whether `-J--enable-native-access` stays in `build.sbt`; poll interval;
`draw` test or not.
