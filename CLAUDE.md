# learning-tui4s

## JVM warnings about `sun.misc.Unsafe`: ignore them

On JDK 25 the JVM prints `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` (and the lines
after it, from `scala.runtime.LazyVals$`, sbt and JLine) when a module starts or first uses a lazy value. This is known
and accepted.

- Do not investigate, silence or work around these warnings (no `--sun-misc-unsafe-memory-access`, no other JVM flag
  for them) unless the author asks.
- Do not count them as a cause when a screen looks wrong, and do not report them as a finding.

Recorded because it came up while looking for a display bug in the `jatatui` and `tui4kyo` modules: in a replay of the
terminal output the warning text landed on the list rows. The author decided to leave it.
