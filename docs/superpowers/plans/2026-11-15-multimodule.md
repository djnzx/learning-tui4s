# FolderViewApp, four stacks in one sbt build – implementation plan

**Spec:** `docs/superpowers/specs/2026-11-15-multimodule-design.md`. Branch `multimodule` (cut from `comparison`).
Sources of the moved code: `git show lanterna:…`, `termflow:…`, `jatatui:…`, `kyo:…` (nothing on those branches changes).
Run each check in a shell; app runs are checked in a real terminal (Terminal.app driven by AppleScript, as before).

## Global constraints

- Scala 3.9.0; same `scalacOptions` as today; `sbt test` passes without a terminal.
- Modules `core`, `lanterna`, `termflow`, `jatatui`, `tui4kyo`; packages `jnz`, `jnz.<stack>`; main `jnz.<stack>.FolderViewApp`.
- No unused template dependency; `core` has no library dependency; scalatest is `Test` scope.
- Do not lose any `.md` content, spec, plan or the `prompt/1.md`.

## Tasks

### 1. Skeleton and `core`
- [x] Root `build.sbt`: `commonSettings`, aggregate root (`publish / skip`), `core` project; delete old root sources
- [x] `core/src/main/scala/jnz/FolderModel.scala` moved unchanged (`git mv`), `FolderModelSpec` moved to `core/src/test`
- [x] `FolderLines` (test first): `FolderLinesSpec` from the pure tests of `FolderViewSpec`, then `FolderLines.lines`
- [x] `sbt core/test` green; commit

### 2. `lanterna`
- [x] Module with dependency `com.googlecode.lanterna:lanterna:3.1.5`, `run / fork`, packages `jnz.lanterna`
- [x] Move `FolderViewApp`, `FolderView.draw`, `SttyTerminal`; `FolderView` uses `FolderLines`
- [x] Move `KeysSpec`, `FolderViewSpec` (draw tests), `SttyTerminalSpec`
- [x] `sbt lanterna/test` green; commit

### 3. `termflow`
- [x] Module with dependency `org.llm4s:termflow:0.4.0`, packages `jnz.termflow`; sources from branch `termflow`
- [x] Tests `KeysSpec`, `FolderViewSpec` (`view` test); `sbt termflow/test` green; commit

### 4. `jatatui`
- [x] Module with `jatatui-core`, `-widgets`, `-crossterm` 0.31.0, `cats-effect`, `fs2-core` (only what compiles), `run / fork`, native access
- [x] Sources and tests from branch `jatatui`; `sbt jatatui/test` green; commit

### 5. `tui4kyo`
- [x] Find the smallest tui4s artifact set that compiles the old app
- [x] Tests first: `KeysSpec` (key → `Action`), `FolderViewSpec` (screen text / bold via `FolderLines`) – red
- [x] Refactor to `core`: `State` in the `SignalRef`, `FolderModel(...)` in `Sync.defer`, `FolderLines` for text; tests green
- [x] `sbt tui4kyo/test` green; commit

### 6. Run each module
- [x] `sbt "lanterna/runMain jnz.lanterna.FolderViewApp"` draws, `j`+Return, `q` exits 0 (Terminal.app)
- [x] same for `termflow`, `jatatui`, `tui4kyo`; fix `fork`/`javaOptions`/`connectInput` per module; record findings (below)
- [x] `sbt test` (all modules) green; commit

### 7. Docs
- [x] README: modules, run commands, tests, known issues (Kyo display bug), Metals section kept verbatim, docs index
- [x] `comparison.md`: one-build note (modules, run commands)
- [x] Check no `.md`/spec/plan/prompt content from the branches is lost (`git diff --stat` against each branch for `*.md`, `docs`, `prompt`)
- [x] Commit

## Findings of task 6 (checks in a real Terminal.app window; first screen after 4–6 s of sbt startup, `q` exits with 0)

- `lanterna`: works with `fork`; `SttyTerminal` opens `/dev/tty`. `stty` code moved to `core` (`Stty`), shared with termflow.
- `jatatui`: needs `connectInput` (crossterm: "Failed to initialize input reader" without it) and the native access flag.
- `tui4kyo`: works with `fork`, `connectInput`, native access and the four Kyo scheduler properties.
- `termflow`: under sbt's fork JLine gets pipes, creates a dumb terminal and the screen stays empty. Fix: its own
  `TtyTerminalBackend` (TermFlow's `TerminalBackend`) on `/dev/tty` with `stty` for raw mode and size, WINCH handler for
  resize. TermFlow closes the key reader at the end while its thread is blocked in `read`; `InputStreamReader.close`
  waits for the next key, so the app seemed to hang after `q`. The reader is wrapped with a `close` that does nothing.
- The forked JVM starts in the module folder (`termflow/`, ...), so the shown folder at start is the module folder.
