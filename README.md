# learning-tui4s

A Scala 3 terminal UI project: a Midnight Commander-style folder browser (`jnz.FolderViewApp`) built on
[TermFlow](https://llm4s.github.io/termflow/) (an Elm-architecture terminal UI framework for Scala 3).
Earlier versions: [tui4s](https://github.com/yuri4n/tui4s)/kyo on the `kyo` branch, jatatui on the `jatatui` branch.

## Run

```shell
sbt stage
target/universal/stage/bin/learning-tui4s
```

`sbt run` does not work: the forked JVM has no tty, so the terminal has no size and the screen stays empty.

Keys: Up/Down (`k`/`j`), PageUp/PageDown, Home/End, Enter or Right to open a folder, Backspace or Left to go up,
F10 or `q` to quit. Needs a real terminal.

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
