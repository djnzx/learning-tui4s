# learning-tui4s

One app, four terminal UI stacks, one sbt build. The app is `FolderViewApp`: a full-screen, responsive, Midnight Commander-style
folder browser (default colors, no configuration; the task is in [`prompt/1.md`](prompt/1.md)). It was written four times on
four libraries and the versions are compared in [`comparison.md`](comparison.md).

| Module | Library | Main class |
|---|---|---|
| `core` | the shared pure part: the model of the folder (`FolderModel`), the text of the screen (`FolderLines`), `stty` helpers (`Stty`); no library dependency | none |
| `lanterna` | [Lanterna](https://github.com/mabe02/lanterna) (Java), `Screen` API | `jnz.lanterna.FolderViewApp` |
| `termflow` | [TermFlow](https://llm4s.github.io/termflow/) (Scala 3, Elm architecture) | `jnz.termflow.FolderViewApp` |
| `jatatui` | [jatatui](https://github.com/oyvindberg/jatatui) (Java port of ratatui) on cats-effect and fs2 | `jnz.jatatui.FolderViewApp` |
| `tui4kyo` | [tui4s](https://github.com/yuri4n/tui4s) on [Kyo](https://getkyo.io) (release candidate) | `jnz.tui4kyo.FolderViewApp` |

Each stack module depends on `core` and on its library only; `jatatui` is the only one that sees cats-effect and fs2.
The four implementations also exist as they were first written, one per branch: `kyo` (tui4s, also `master`), `jatatui`,
`termflow` and `lanterna` (`comparison` adds `comparison.md` to the Lanterna one).

## Run

Needs a real terminal (iTerm2, Terminal.app, the terminal of an IDE) and JDK 25. Start one module from the project folder:

```shell
sbt "lanterna/runMain jnz.lanterna.FolderViewApp"
sbt "termflow/runMain jnz.termflow.FolderViewApp"
sbt "jatatui/runMain jnz.jatatui.FolderViewApp"
sbt "tui4kyo/runMain jnz.tui4kyo.FolderViewApp"
```

The first screen appears after sbt has started (4–6 s on the development machine). Keys: Up/Down (`k`/`j`),
PageUp/PageDown, Home/End, Enter or Right to open a folder, Backspace or Left to go up, F10 or `q` to quit.

Notes:

- Under sbt the list of `jatatui` and `tui4kyo` can lose the rows under the selection (see the next section; use `java`).
- `runMain` is forked (`run / fork`, in `build.sbt`): the app has its own JVM and starts in the folder of its module
  (`lanterna/`, `termflow/`, ...), so that is the first folder shown.
- sbt pipes the stdin and stdout of a forked app, which shows an empty screen in a real terminal. `lanterna` and `termflow`
  therefore open `/dev/tty` themselves (`SttyTerminal`, `TtyTerminalBackend`) and read the size and the raw mode with `stty`
  (`Stty` in `core`). `jatatui` and `tui4kyo` run with `connectInput`.
- Lanterna's own query for the terminal size waits for ever in a terminal that does not answer the cursor-position request;
  `SttyTerminal` reads the size from `stty size` instead.
- `jatatui` (JNI to the Rust crossterm crate) and `tui4kyo` need `--enable-native-access=ALL-UNNAMED`; `tui4kyo` also sets
  the Kyo scheduler properties of tui4s. Both are in `build.sbt`.

## Run without sbt, with `java`

Prefer this to `sbt runMain` when the list loses rows: while sbt runs a forked app it writes its own terminal sequences, and
after every frame it erases the screen from the cursor down (`ESC[0J`, seen in the output of `jatatui` under sbt). The cursor
is then one row below the selection, so after moving up only one entry below the cursor stays visible. A plain `java` has
no such output and the app starts in the folder where it is run, not in the module folder.

Run each block in the project folder. The app shows the folder where it is started, here the project folder; to browse
another one, put `cd <folder>` before the `java` line (the classpath is absolute, so it still works). The classpath comes
from sbt (`export <module>/Runtime/fullClasspath`: the module's classes, `core` and the libraries) and is read again at every
start, so a build change is picked up after `sbt compile`. `tail -1` keeps the classpath, the last line: sbt may print other
lines to stdout before it (for example JVM flags set by `JAVA_TOOL_OPTIONS` or `.jvmopts`).

`lanterna`:

```shell
sbt -batch compile
CP=$(sbt -batch -error "export lanterna/Runtime/fullClasspath" | tail -1)
java -cp "$CP" jnz.lanterna.FolderViewApp
```

`termflow` (the option only silences JLine's native-access warning):

```shell
sbt -batch compile
CP=$(sbt -batch -error "export termflow/Runtime/fullClasspath" | tail -1)
java --enable-native-access=ALL-UNNAMED -cp "$CP" jnz.termflow.FolderViewApp
```

`jatatui`:

```shell
sbt -batch compile
CP=$(sbt -batch -error "export jatatui/Runtime/fullClasspath" | tail -1)
java --enable-native-access=ALL-UNNAMED -cp "$CP" jnz.jatatui.FolderViewApp
```

`tui4kyo` (the four `kyo.scheduler` properties are the ones tui4s asks for):

```shell
sbt -batch compile
CP=$(sbt -batch -error "export tui4kyo/Runtime/fullClasspath" | tail -1)
java --enable-native-access=ALL-UNNAMED \
  -Dkyo.scheduler.cycleIntervalNs=10000000 \
  -Dkyo.scheduler.blockingMonitorIntervalNs=20000000 \
  -Dkyo.scheduler.blockingMonitorMinIntervalNs=20000000 \
  -Dkyo.scheduler.regulator.concurrencyCollectIntervalMs=100 \
  -cp "$CP" jnz.tui4kyo.FolderViewApp
```

## Test

```shell
sbt test                 # all modules, no terminal needed
sbt core/test            # the model and the screen text
sbt lanterna/test        # keys, drawing on Lanterna's virtual terminal, stty size
sbt termflow/test        # keys, the view tree
sbt jatatui/test         # keys, drawing into a jatatui buffer
sbt tui4kyo/test         # keys, the app on tui4s's headless run
```

## Known issues

- `tui4kyo`: the author reports a display bug in the tui4s/Kyo version that the Lanterna and TermFlow versions do not have.
  It was not investigated; the refactoring onto `core` keeps the behavior of the original `kyo` version.
- tui4s is a release candidate (`0.1.1-RC1`, on Kyo and kyo-ui release candidates).

## Documents

- [`comparison.md`](comparison.md): the four stacks side by side (facts, problems met, metrics).
- [`prompt/1.md`](prompt/1.md): the task.
- [`docs/superpowers/specs`](docs/superpowers/specs) and [`docs/superpowers/plans`](docs/superpowers/plans): the designs and
  plans, one pair for the jatatui version, one pair for the Lanterna version and one pair for this multi-module build. The
  kyo and TermFlow versions were written without a spec or a plan.

## Metals

[Metals](https://scalameta.org/metals/) is the Scala language server. Here it also runs as an MCP server (`metals-mcp`), so Claude Code can use the compiler, build and symbol tools of this project.

### 1. Install

Requires Homebrew and sbt (`sbt` 1.13.1 is pinned in `project/build.properties`).

```shell
brew install metals
brew install sbt
```

Homebrew installs `openjdk@25` as a dependency of `metals`. Verify:

```shell
metals --version
metals-mcp --version
sbt --version
```

### 2. Generate the build server connection

Metals talks to sbt over BSP. `.bsp/sbt.json` is already committed; regenerate it after changing `build.sbt` or `project/*.sbt`:

```shell
cd ~/dev/pp/learning-tui4s
sbt bspConfig
```

Metals also writes `project/metals.sbt` (auto-generated, `DO NOT EDIT`) for the bloop plugin. Leave it as it is.

### 3. Register the MCP server

The project config lives in `.mcp.json`. To recreate it:

```shell
cd ~/dev/pp/learning-tui4s
claude mcp add --scope project metals -- /opt/homebrew/bin/metals-mcp \
  --workspace "$PWD" --transport stdio
```

`.mcp.json` contains an absolute path to your machine. Do not commit it unless you change it to a path that works for everyone.

### 4. Approve and check

Start Claude Code in the project and approve the `metals` server when prompted:

```shell
cd ~/dev/pp/learning-tui4s
claude
```

Check the status from a shell:

```shell
claude mcp list
```

`metals` should show `✔ Connected` (or `Pending approval` until you approve it inside Claude Code).

### 5. Quick check of the server

Send an MCP `initialize` request by hand. The server keeps running after its input closes, so the command starts it in the background, waits, and stops it. The server first prints `notifications/tools/list_changed` lines, so the check searches for the response with `id` 1:

```shell
cd ~/dev/pp/learning-tui4s
( printf '%s\n' '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"check","version":"0"}}}'; sleep 10 ) \
  | metals-mcp --workspace "$PWD" --transport stdio >handshake.out 2>metals-mcp.err &
PID=$!
sleep 25
kill $PID
grep '"id":1' handshake.out
rm handshake.out
```

A line with `"serverInfo":{"name":"learning-tui4s-metals"...}` means the server works. If nothing is printed, read the log:

```shell
cat metals-mcp.err
```

### 6. Compile and test from the command line

```shell
cd ~/dev/pp/learning-tui4s
sbt compile
sbt test
```

### Troubleshooting

- **`metals-mcp: command not found`**: run `brew link metals` or use the full path `/opt/homebrew/bin/metals-mcp`.
- **Wrong JDK**: set `JAVA_HOME` to `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`, or pass `--java-home <path>` to `metals-mcp`.
- **Stale build information**: run `sbt bspConfig` again and restart Claude Code.
