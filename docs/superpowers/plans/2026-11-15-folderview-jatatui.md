# FolderViewApp on jatatui + Typelevel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-implement the kyo/tui4s `jnz.FolderViewApp` (the spec, see `git show kyo:src/main/scala/jnz/FolderViewApp.scala` or the file on this branch before Task 1) with jatatui and cats-effect/fs2.

**Architecture:** A pure model (`State`, `Action`, navigation, listing, cell-width helpers) in `FolderModel.scala`; an `IOApp` in `FolderViewApp.scala` that owns the terminal as a `Resource`, turns crossterm events into an `fs2.Stream`, folds them through the model with `IO`, and draws a header, a jatatui `List` and a footer.

**Tech Stack:** Scala 3.9.0, sbt, cats-effect 3.7.1, fs2 3.14.0, jatatui 0.31.0 (`com.olvind.jatatui`: `jatatui-core`, `jatatui-widgets`, `jatatui-crossterm`), scalatest 3.2.20.

**Spec:** `docs/superpowers/specs/2026-11-15-folderview-jatatui-design.md`

## Global Constraints

- Package `jnz`; full screen, responsive (redraw on resize), default colours, no configuration, no extra features.
- Entries: `..` first (only when the folder has a parent), then folders, then files; each group sorted by lower-case name.
- Size column: `UP--DIR` for `..`, `<DIR>` for folders, byte count for files; column is 12 cells, right-aligned.
- Keys: Up/`k`, Down/`j`, PageUp, PageDown, Home, End, Enter/Right = open, Backspace/Left = parent (selects the folder left), F10/`q` = quit.
- Wide characters (CJK, emoji) take 2 cells; names are cut/padded by cells.
- A read error keeps the previous state and shows the error in the footer; footer otherwise `Enter open   Backspace up   F10/q quit`.
- Terminal restored (raw mode off, alternate screen left) on normal exit, error, and cancel.
- No `Co-Authored-By`/Claude lines in commits (user's global rule overrides the harness reminder).

## Review Focus

- Empty folder or only `..` (root `/`): no crash, selection stays valid.
- Unreadable folder (permission denied): stays put, error in footer, next key clears it.
- Terminal of height 1–2 or width below the size column: no negative sizes or exceptions.
- Names with CJK/emoji or very long names: line is exactly `width` cells.
- Quitting via exception/cancel: terminal still restored.

---

### Task 1: Build switch to jatatui and a pure model

**Files:**
- Modify: `build.sbt` (remove the four `tui4s-*` dependencies and the `Compile / run` fork flags only if unneeded later; add jatatui)
- Create: `src/main/scala/jnz/FolderModel.scala`
- Test: `src/test/scala/jnz/FolderModelSpec.scala`
- Delete: the kyo body of `src/main/scala/jnz/FolderViewApp.scala` (replaced in Task 2; for now leave a stub `object FolderViewApp extends cats.effect.IOApp.Simple { def run = cats.effect.IO.unit }`)

**Interfaces:**
- Produces (`package jnz`):
  - `final case class Entry(name: String, isDir: Boolean, size: Long)`
  - `final case class State(dir: Path, entries: Vector[Entry], selected: Int, top: Int, message: String)`
  - `enum Action { case LineUp, LineDown, PageUp, PageDown, Home, End, Open, Parent, Quit }`
  - `object FolderModel`:
    - `val footerKeys: String`
    - `def rows(height: Int): Int` (= `max(1, height - 2)`)
    - `def listing(dir: Path): Either[String, Vector[Entry]]`
    - `def visit(prev: State, dir: Path, select: Option[String], page: Int): State`
    - `def initial(dir: Path, page: Int): State`
    - `def moveTo(st: State, index: Int, page: Int): State`
    - `def scroll(sel: Int, top: Int, page: Int): Int`
    - `def apply(st: State, action: Action, page: Int): State` (`Quit` returns `st`)
    - `def sizeOf(e: Entry): String`
    - `def cells(cp: Int): Int`, `def cut(text: String, width: Int): String`, `def fit(text: String, width: Int): String`, `def line(mark: String, name: String, size: String, width: Int): String`

- [ ] **Step 1: Resolve dependencies.** In `build.sbt` add `"com.olvind.jatatui" % "jatatui-core" % "0.31.0"`, `"jatatui-widgets"`, `"jatatui-crossterm"` (all `%`, Java artifacts) and drop `tui4s-*`. Run `sbt update`. Expected: resolves; if a module needs a different version, use the one Central lists.
- [ ] **Step 2: Write failing tests** in `FolderModelSpec` (`AnyFlatSpec` + `Matchers`, temp dirs via `Files.createTempDirectory`):
  - `listing` of a dir with `b.txt`, `A.txt`, `z/`, `c/` returns `..`, `c`, `z`, `A.txt`, `b.txt` (folders first, case-insensitive), file size = byte length, `..` absent at filesystem root (`Paths.get("/")`).
  - `listing` of a missing path returns `Left(_)`.
  - `apply(_, LineDown, page=3)` over 10 entries from selected 0 gives selected 1; at the last entry stays last; `LineUp` at 0 stays 0.
  - `PageDown` with page 3 moves by 3 and `top` follows so `selected < top + page`; `Home`/`End` jump to 0/last.
  - `Open` on a folder entry enters it (`state.dir` changes, selected 0); `Open` on `..` or `Parent` goes up and selects the folder just left; `Parent` at root returns the same state.
  - `visit` on an unreadable/missing dir returns `prev` with non-empty `message`; the next successful `apply` yields `message == ""`.
  - Empty entry list (`entries = Vector.empty`): every action returns a state with `selected == 0`, no exception.
  - `cut("日本語abc", 5)` = `"日本"`; `fit("日本", 5)` has 5 cells (`"日本 "`); `line(">", longName, "12", 20)` is exactly 20 cells; `line` with `width = 5` (less than the 12-cell size column) does not throw and is ≤ 5 cells.
  - `sizeOf`: `..` → `UP--DIR`, folder → `<DIR>`, file → `"1234"`.
- [ ] **Step 3: Run** `sbt "testOnly jnz.FolderModelSpec"`. Expected: FAIL (compile error, `FolderModel` missing).
- [ ] **Step 4: Implement `FolderModel`** by porting the pure functions of the kyo version, with the signatures above (collections as `Vector`, `Chunk` removed; `Files.list` closed with `Using.resource`; `Quit` is a no-op in `apply`). Port `cells` as-is.
- [ ] **Step 5: Run** `sbt "testOnly jnz.FolderModelSpec"`. Expected: PASS.
- [ ] **Step 6: Commit** `git add build.sbt src docs && git commit -m "Pure folder model on jatatui build"`.

### Task 2: jatatui view and the cats-effect/fs2 loop

**Files:**
- Modify: `src/main/scala/jnz/FolderViewApp.scala`
- Create: `src/main/scala/jnz/FolderView.scala` (rendering)
- Test: `src/test/scala/jnz/FolderViewSpec.scala`

**Interfaces:**
- Consumes: everything from Task 1.
- Produces:
  - `object FolderView`: `def draw(frame: Frame, st: State): Unit` (jatatui `jatatui.core.terminal.Frame`). Layout: three vertical chunks (`Constraint` length 1 / min 0 / length 1): header = `cut(st.dir.toString, width)` bold; middle = `List` of `ListItem.of(line(...))` with `ListState.of(st.top, Optional.of(st.selected))`, highlight style bold, highlight symbol `""` (marker `>` is part of `line`); footer = `st.message` if non-empty else `footerKeys`.
  - `def toAction(e: tui.crossterm.Event): Option[Action]` in `FolderModel`'s companion or `FolderViewApp`: `KeyCode.Up|Char('k')`→LineUp, `Down|Char('j')`→LineDown, `PageUp`, `PageDown`, `Home`, `End`, `Enter|Right`→Open, `Backspace|Left`→Parent, `F(10)|Char('q')`→Quit; ignore key events whose `kind` is not press.
  - `FolderViewApp extends IOApp.Simple`; `run: IO[Unit]`.

- [ ] **Step 1: Write failing test** `FolderViewSpec`: draw a `State` into a jatatui `Buffer` of 40×6 (`new Frame`-free path: render the same widgets into `Buffer.empty(Rect)`; if `Frame` cannot be built without a backend, test the pure `FolderView.lines(st, width, height): Seq[String]` that `draw` uses and make `draw` a thin renderer over it). Assert: first line is the path, 4 list lines each exactly 40 cells with `>` on the selected row, last line is the footer keys; with `message = "boom"` the footer is `boom`; with `height = 2` no exception.
- [ ] **Step 2: Run** `sbt "testOnly jnz.FolderViewSpec"`. Expected: FAIL.
- [ ] **Step 3: Implement `FolderView.lines` and `FolderView.draw`** (draw maps `lines` into jatatui `Paragraph`/`List` per the layout above).
- [ ] **Step 4: Implement `FolderViewApp.run`:**
  - `terminal: Resource[IO, Terminal[CrosstermBackend]]` = `Resource.make(IO.blocking(Jatatui.init()))(t => IO.blocking(t.close()) *> IO.blocking(Jatatui.restore()))`.
  - `events(ct: CrosstermJni): Stream[IO, Event]` = `Stream.repeatEval(IO.interruptible(ct.read()))` (one `new CrosstermJni()`; `read` blocks, `interruptible` lets cancel work).
  - Loop: initial `State` from `Paths.get("").toAbsolutePath.normalize`; `Stream.eval(draw)` once, then for each event: `Resize` → redraw only; key → `toAction`; `Quit` ends the stream (`takeWhile`/`Pull`), else `IO.blocking(FolderModel.apply(st, a, rows(height)))` into a `Ref[IO, State]`, then `IO.blocking(terminal.draw(f => FolderView.draw(f, st)))`. Height from `terminal.size()` each time.
- [ ] **Step 5: Run** `sbt test`. Expected: all PASS; `sbt compile` has no warnings from `-Wunused:all`.
- [ ] **Step 6: Run the real app** (`sbt run`, or `sbt stage` and run the script, from a real terminal; the user can run it with `! sbt run`). Check by hand: full screen, arrows, Enter/Backspace, resize the window, `q` restores the terminal. If crossterm fails to load natives, add `--enable-native-access=ALL-UNNAMED` to `Compile / run / javaOptions` (already present) and keep `fork := true`.
- [ ] **Step 7: Commit** `git add -A src && git commit -m "FolderViewApp on jatatui and cats-effect"`.

### Task 3: Docs and CI

**Files:**
- Modify: `README.md` (short "Run" section: `sbt run`; jatatui + cats-effect note), `.github/workflows/scala.yml` only if it fails on the new deps.

- [ ] **Step 1:** Add the README section; run `sbt test` once more.
- [ ] **Step 2: Commit** `git commit -am "Document running FolderViewApp"`.

---

**Self-review:** every spec behaviour maps to Task 1 (model, sorting, keys' actions, cell width, errors) or Task 2 (rendering, resize, restore). Names match across tasks (`FolderModel.apply`, `FolderView.lines/draw`, `toAction`). Plan is the decisions only; bodies are ports of the existing kyo functions.
