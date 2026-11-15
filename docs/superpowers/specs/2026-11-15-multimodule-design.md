# FolderViewApp, four stacks in one sbt build – design

## Intent

One sbt project holds all four implementations of `FolderViewApp` (a full-screen, responsive, Midnight Commander-style
folder browser; spec in `prompt/1.md`), so each can be built, tested and started with `sbt "<module>/runMain <class>"`,
and compared (`comparison.md`). Until now each lived on its own branch (`kyo`, `jatatui`, `termflow`, `lanterna`).

## Goals

- Modules: `core`, `lanterna`, `termflow`, `jatatui`, `tui4kyo`. Every stack module depends on `core` only.
- `core` holds what the four implementations shared: the pure model (`FolderModel`: `Entry`, `State`, `Action`, listing,
  navigation, wide-character `cut`/`fit`, `line`) and the pure screen text (`FolderLines.lines`), with their tests.
- Each module has its own tests and declares only the dependencies it uses. The unused template dependencies (http4s,
  circe, mockito, scalacheck, pprint, ...) are dropped. `core` has no library dependency, scalatest is `Test` only.
- Nothing is lost: all specs, plans, `prompt/1.md`, `comparison.md` and the README text of every branch stay in the repo.
- The `kyo` version is refactored to use `core` (it had no separate model). Its behavior is kept, including the display
  bug (below).
- README explains how to run each module.

## Non-goals / anti-goals

- No new features, colors, configuration. No change to the four implementations beyond what moving them needs
  (package names, use of `core`, `build.sbt` per module).
- The tui4s/Kyo display bug is not investigated or fixed. The author reports it for the tui4s/Kyo version and not for
  Lanterna and TermFlow; it stays and is documented as a known issue. Anti-goal: hiding it.

## Constraints

- Scala 3.9.0, sbt 1.13.1 (pinned), JDK 25, macOS used for checks. `sbt test` must pass with no terminal (CI).
- Each app needs a real terminal; under sbt it runs forked (`run / fork`). Lanterna and jatatui also need the tty
  handling found earlier (`SttyTerminal` opens `/dev/tty`).

## Approaches (picked: 1)

1. **Multi-module build, `core` plus one module per stack** (picked, as the author asked).
2. One module with four packages: one dependency set for all, which violates "minimal dependencies per artifact".
3. Keep four branches: no combined run, no shared core.

The jatatui module keeps cats-effect and fs2 (its approved design, `2026-11-15-folderview-jatatui-design.md`); the other
modules do not see them (author's choice).

## Design

- Layout: `core/`, `lanterna/`, `termflow/`, `jatatui/`, `tui4kyo/`, each with `src/main` and `src/test`; root
  `build.sbt` with shared settings (`commonSettings`: Scala version, compiler options, scalatest `Test`) and an
  aggregating root that is not run.
- Packages: `jnz` (core), `jnz.lanterna`, `jnz.termflow`, `jnz.jatatui`, `jnz.tui4kyo`. Main classes
  `jnz.<stack>.FolderViewApp`. Single `package` clauses, so `import jatatui.*` still means the library.
- Run: `sbt "lanterna/runMain jnz.lanterna.FolderViewApp"` and the same for the other three.
- `core`: `FolderModel.scala` (identical on the three branches that had it; the kyo version's copy is replaced by it) and
  `FolderLines.scala` (`lines(state, width, height): Vector[(String, Boolean)]`, bold flag per row; was
  `FolderView.lines`). Tests: `FolderModelSpec`, `FolderLinesSpec`.
- Module contents (as built on the branches): `lanterna` = `FolderViewApp`, `FolderView.draw`, `SttyTerminal`;
  `termflow` = `FolderViewApp` (`TuiApp`), `FolderView.view`; `jatatui` = `FolderViewApp` (`IOApp`, fs2), `FolderView`
  (widgets); `tui4kyo` = `FolderViewApp` on Kyo signals, state is `core`'s `State` in a `SignalRef`, text from
  `FolderLines`, listing and navigation through `Sync.defer(FolderModel(...))`.
- Tests per module: key mapping (`KeysSpec`), the module's drawing (`FolderViewSpec`), and `SttyTerminalSpec` for Lanterna.
- Docs: `docs/superpowers/specs` and `plans` keep every existing file; this design and its plan are added there.
  README: what the project is, modules, run commands, tests, Metals section (unchanged), known issues. `comparison.md`
  gets a short "one build" note.

## Left to the builder

Exact tui4s artifacts needed by `tui4kyo` (smallest set that compiles and tests), whether `termflow` runs forked from sbt
or needs `JavaAppPackaging`, the sbt scoping of `fork` for `runMain`, per-module `javaOptions`.
