package jnz

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal.CtrlCBehaviour
import com.googlecode.lanterna.terminal.ansi.UnixTerminal
import java.nio.charset.Charset
import scala.util.Try

/** A Unix terminal that reads its size with `stty size` instead of Lanterna's cursor-position query, which blocks for good in a terminal that does not answer it. Ctrl+C reaches the app as a key. */
final class SttyTerminal extends UnixTerminal(System.in, System.out, Charset.defaultCharset, CtrlCBehaviour.TRAP):

  override protected def findTerminalSize(): TerminalSize =
    SttyTerminal.query().getOrElse(SttyTerminal.fallback)

object SttyTerminal:

  val fallback: TerminalSize = TerminalSize(80, 24)

  /** `stty size` prints `rows cols`. */
  def parseSize(output: String): Option[TerminalSize] =
    output.trim.split("\\s+").flatMap(_.toIntOption) match
      case Array(rows, cols) if rows > 0 && cols > 0 => Some(TerminalSize(cols, rows))
      case _                                         => None

  /** Asks the controlling terminal, as stdin is not a file here. */
  def query(): Option[TerminalSize] =
    Try {
      val process = ProcessBuilder("sh", "-c", "stty size < /dev/tty").redirectErrorStream(true).start()
      val output  = String(process.getInputStream.readAllBytes())
      process.waitFor()
      output
    }.toOption.flatMap(parseSize)
