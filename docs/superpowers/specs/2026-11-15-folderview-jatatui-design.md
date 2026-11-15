# FolderViewApp on jatatui + Typelevel (short design)

## Intent
Re-implement `jnz.FolderViewApp` (kyo/tui4s version, kept in git history and on branch `kyo`) as a spec: a full-screen,
responsive, Midnight Commander-style folder browser. Purpose: learn jatatui with the Typelevel stack.

## Goals
- Same behaviour as the spec: `..`, folders, files (each group sorted by name, case-insensitive); selection, scrolling that
  follows the selection; sizes on the right (`UP--DIR`, `<DIR>`, bytes); error of the last step in the footer.
- Keys: Up/k, Down/j, PgUp, PgDn, Home, End, Enter/Right (open), Backspace/Left (up, selecting the folder left), F10/q (quit).
- Full screen, redraws on resize, default colours, no configuration.

## Non-goals / anti-goals
No other functionality, no config, no colours/themes. No imperative mutable state outside `Ref`/resources.

## Constraints
- Scala 3.9.0, sbt; cats-effect 3 + fs2 (already in `build.sbt`). tui4s/kyo dependencies are removed.
- jatatui (`com.olvind.jatatui`, `jatatui-core`, `-crossterm`, `-widgets`; latest on Central 0.31.0, Java 21+).
- Native access flag for the forked JVM stays in `Compile / run / javaOptions` if crossterm needs it.

## Approaches
1. **IOApp + Ref + fs2 event stream (picked).** Events from a blocking poll become a `Stream`; fold with pure `update`; redraw.
2. Plain recursive `IO` loop, no fs2: simpler, fewer Typelevel idioms. Rejected by the user.

## Design
- `jnz/FolderModel.scala` (pure): `Entry`, `State`, `Action`, `update(State, Action, page): State`, `listing(Path)`
  (the only effectful-ish part, wrapped in `IO`), cell-width helpers `cut`/`fit`, `line`.
- `jnz/FolderViewApp.scala`: `IOApp`; terminal as `Resource` (raw mode + alternate screen, restored on exit/crash);
  `Stream` of key/resize events (blocking poll on `IO.blocking`); `evalScan`/`Ref` loop that applies `update`, performs
  directory reads through `IO`, and draws with jatatui `Block`/`List`/`Paragraph` (header = path, list, footer = keys/error).
- Tests (scalatest): `update` navigation, scrolling, sort order, `..` handling, cell-width cutting. A rendering test with a
  jatatui test backend only if one exists.

## Left to the builder
Exact jatatui API usage (stateful `List` widget vs manual rows), event type mapping, whether crossterm needs
`--enable-native-access`.
