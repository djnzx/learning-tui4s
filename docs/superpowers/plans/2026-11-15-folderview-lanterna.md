# FolderViewApp on Lanterna Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace TermFlow with Lanterna in `jnz.FolderViewApp`, keeping behavior, keys and the pure model unchanged.

**Architecture:** `FolderModel` and `FolderView.lines` stay as they are. `FolderViewApp.toAction` maps a Lanterna `KeyStroke` to an `Action`. `FolderView.draw` paints `lines` onto a Lanterna `Screen`. `FolderViewApp.main` runs a poll loop: resize, draw, poll a key, update the `State`.

**Tech Stack:** Scala 3.9.0, sbt, Lanterna 3.1.3 (`com.googlecode.lanterna:lanterna`), scalatest 3.2.20.

**Spec:** `docs/superpowers/specs/2026-11-15-folderview-lanterna-design.md`

## Global Constraints

- Keys: ArrowUp/`k` LineUp, ArrowDown/`j` LineDown, PageUp, PageDown, Home, End, Enter/ArrowRight Open, Backspace/ArrowLeft Parent, F10/`q`/Ctrl+C Quit.
- Default colors, no configuration, no new features, no mouse, no Lanterna GUI widgets, no Elm `Msg`/`update` layer.
- `FolderModel.scala` and `FolderView.lines` are not changed.
- Bold only for the header (row 0) and the selected entry, via `SGR.BOLD`.
- `sbt run` has no tty; the app is run from `sbt stage` (`target/universal/stage/bin/learning-tui4s`).
- The code style of the repo (scalafmt, `-Wunused:all`) holds; no new warnings.

## Review Focus

- Terminal smaller than the layout (height 1 or 2, width under 10): `draw` must not throw.
- Resize while idle: the screen redraws without a key press.
- Unknown keys (`x`, Tab, Escape) and `KeyType.EOF`: ignored for `x`/Tab/Escape; EOF (terminal closed) quits instead of looping forever.
- An unreadable folder: the message shows in the footer, the app stays running (already in `FolderModel`; the loop must not catch and swallow other exceptions).
- The terminal is restored (`stopScreen`) on quit and on an exception.

---

### Task 1: Lanterna dependency and `toAction` on `KeyStroke`

**Files:**
- Modify: `build.sbt` (the `libraryDependencies` block)
- Modify: `src/main/scala/jnz/FolderViewApp.scala`
- Modify: `src/test/scala/jnz/KeysSpec.scala`

**Interfaces:**
- Consumes: `Action` (`FolderModel.scala`).
- Produces: `FolderViewApp.toAction(key: KeyStroke): Option[Action]`.

- [ ] **Step 1: Check the Lanterna version.** Confirm `com.googlecode.lanterna:lanterna` 3.1.3 is the latest 3.1.x on Maven Central (`mcp__metals__find-dep` or the Maven search); use the latest 3.1.x if newer.
- [ ] **Step 2: Rewrite `KeysSpec` on `KeyStroke`.** Same two tests ("map the keys of the spec", "ignore other keys"), keys built as `KeyStroke(KeyType.ArrowUp)` etc.; `KeyStroke('k', false, false)` for characters; Ctrl+C is `KeyStroke('c', true, false)` and maps to `Some(Action.Quit)`. Ignored: `KeyStroke('x', false, false)`, `KeyStroke(KeyType.Tab)`, `KeyStroke(KeyType.Escape)`. Add: `KeyStroke(KeyType.EOF)` maps to `Some(Action.Quit)`.
- [ ] **Step 3: Run it to see it fail.** `sbt "testOnly jnz.KeysSpec"`; expected: compile error (`toAction` takes `InputKey`).
- [ ] **Step 4: Swap the dependency.** In `build.sbt` replace the TermFlow line and its comment with `"com.googlecode.lanterna" % "lanterna" % "3.1.3"` (single `%`, Java artifact) and a doc comment `Lanterna: terminal screen and key input`. Update the comment above `enablePlugins(JavaAppPackaging)` to say Lanterna instead of TermFlow. Keep `Universal / javaOptions` for now (Task 3 decides).
- [ ] **Step 5: Implement `def toAction(key: KeyStroke): Option[Action]`** in `FolderViewApp`, matching on `key.getKeyType` (`ArrowUp`, `ArrowDown`, `PageUp`, `PageDown`, `Home`, `End`, `Enter`, `ArrowRight`, `Backspace`, `ArrowLeft`, `F10`, `EOF`, `Character`); for `Character` use `key.getCharacter` (`'k'`, `'j'`, `'q'`, and `'c'` with `key.isCtrlDown`). Temporarily remove the TermFlow `Model`/`Msg`/`init`/`update`/`view`/`toMsg`/`main` bodies so the file compiles (`main` is rebuilt in Task 3; keep an empty `def main(args: Array[String]): Unit = ()`). Delete `FolderView.view` and its TermFlow imports; remove the `view` test in `FolderViewSpec` (replaced in Task 2).
- [ ] **Step 6: Run the tests.** `sbt test`; expected: all `FolderModelSpec`, `FolderViewSpec` (`lines` tests) and `KeysSpec` pass.
- [ ] **Step 7: Commit.** `git add -A && git commit -m "Lanterna dependency, KeyStroke mapping"`

### Task 2: `FolderView.draw` on a Lanterna `Screen`

**Files:**
- Modify: `src/main/scala/jnz/FolderView.scala`
- Modify: `src/test/scala/jnz/FolderViewSpec.scala`

**Interfaces:**
- Consumes: `FolderView.lines(st: State, width: Int, height: Int): Vector[(String, Boolean)]`.
- Produces: `FolderView.draw(screen: Screen, st: State): Unit` – clears the back buffer, writes `lines` for `screen.getTerminalSize` (one `putString` per row, bold rows with `SGR.BOLD`), calls `screen.refresh()`. It does not call `doResizeIfNecessary`.

- [ ] **Step 1: Write the failing tests** in `FolderViewSpec` under `"draw"`, on `TerminalScreen(DefaultVirtualTerminal(TerminalSize(40, 6)))` after `startScreen()`:
  - "paints the path in bold on row 0 and the selected entry in bold": `screen.getFrontCharacter(TerminalPosition(0, 0))` is `'/'` with `getModifiers.contains(SGR.BOLD)`; the character at `(0, 2)` is `'>'` for `state(selected = 1)` and bold; at `(0, 1)` it is `' '` and not bold.
  - "paints the footer on the last row": the last row read back as a string `.trim` equals `FolderModel.footerKeys`.
  - "does not fail on tiny terminals": sizes `(1, 1)` and `(4, 2)` run `draw` without an exception.
  A helper `rowText(screen, y, width)` reads `getFrontCharacter` cells into a string.
- [ ] **Step 2: Run to see it fail.** `sbt "testOnly jnz.FolderViewSpec"`; expected: compile error, `draw` not defined.
- [ ] **Step 3: Implement `draw`** in `FolderView.scala` with `screen.newTextGraphics()`: `clear()`, then per row `tg.clearModifiers(); if bold then tg.enableModifiers(SGR.BOLD); tg.putString(0, y, text)`, then `refresh()`. Remove the TermFlow imports.
- [ ] **Step 4: Run the tests.** `sbt test`; expected: all pass.
- [ ] **Step 5: Commit.** `git add -A && git commit -m "Draw folder view on a Lanterna screen"`

### Task 3: Main loop, README, manual run

**Files:**
- Modify: `src/main/scala/jnz/FolderViewApp.scala`
- Modify: `README.md`
- Modify: `build.sbt` (only if the run needs it)

**Interfaces:**
- Consumes: `toAction`, `FolderView.draw`, `FolderModel.initial(start: Path, rows: Int): State`, `FolderModel.apply(state: State, action: Action, rows: Int): State`, `FolderModel.rows(height: Int): Int`.
- Produces: `FolderViewApp.main(args: Array[String]): Unit`.

- [ ] **Step 1: Implement `main`.** `DefaultTerminalFactory().createScreen()`, `startScreen()`; the loop lives in a private `loop(screen: Screen, state: State): Unit` (tail-recursive): `screen.doResizeIfNecessary()`, `FolderView.draw(screen, state)`, then `screen.pollInput()`; on `null` sleep about 20 ms and loop; on a key use `toAction`: `Some(Action.Quit)` returns, `Some(a)` loops with `FolderModel(state, a, FolderModel.rows(screen.getTerminalSize.getRows))`, `None` loops unchanged. The initial state is `FolderModel.initial(Paths.get("").toAbsolutePath.normalize, FolderModel.rows(height))`. `main` wraps `loop` in `try … finally screen.stopScreen()`.
- [ ] **Step 2: Compile and test.** `sbt test`; expected: all pass, no new warnings.
- [ ] **Step 3: Run in a real terminal.** `sbt stage` then `target/universal/stage/bin/learning-tui4s` (ask the user to run it with `! target/universal/stage/bin/learning-tui4s` if the tool has no tty). Check: the list shows, arrows/`j`/`k`/Enter/Backspace navigate, resizing the window redraws, `q` restores the terminal. If the JVM prints a native-access warning, keep `-J--enable-native-access=ALL-UNNAMED`; if it is not needed, remove the `Universal / javaOptions` lines and their comment.
- [ ] **Step 4: Update `README.md`:** intro names Lanterna instead of TermFlow; the earlier-versions line lists `termflow` next to `kyo` and `jatatui`; the "`sbt run` does not work" explanation stays and says it for Lanterna's terminal.
- [ ] **Step 5: Commit.** `git add -A && git commit -m "Lanterna main loop, README"`
