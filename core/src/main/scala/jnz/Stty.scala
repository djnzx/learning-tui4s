package jnz

import scala.util.Try

/** The controlling terminal of the process through `stty`, for apps that run forked under sbt, where stdin and stdout are pipes and not the terminal. */
object Stty:

  /** `stty size` prints `rows cols`; the result is (columns, rows). */
  def parseSize(output: String): Option[(Int, Int)] =
    output.trim.split("\\s+").flatMap(_.toIntOption) match
      case Array(rows, cols) if rows > 0 && cols > 0 => Some((cols, rows))
      case _                                         => None

  /** The size of the terminal as (columns, rows). */
  def size(): Option[(Int, Int)] = run("size").flatMap(parseSize)

  /** The settings of the terminal in the form that `restore` takes back. */
  def save(): Option[String] = run("-g").map(_.trim).filter(_.nonEmpty)

  /** Reads keys one by one without echo and without signals, so Ctrl+C reaches the app as a key. */
  def raw(): Option[String] = run("-icanon", "-echo", "-isig", "min", "1", "time", "0")

  def restore(saved: String): Option[String] = run(saved)

  /** Runs `stty <args> < /dev/tty` and gives its output; None when it fails (no terminal). */
  private def run(args: String*): Option[String] =
    Try {
      val process = ProcessBuilder("sh", "-c", s"stty ${args.mkString(" ")} < /dev/tty").redirectErrorStream(true).start()
      val output  = String(process.getInputStream.readAllBytes())
      if process.waitFor() == 0 then Some(output) else None
    }.toOption.flatten
