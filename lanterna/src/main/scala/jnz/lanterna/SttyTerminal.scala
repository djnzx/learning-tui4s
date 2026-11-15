package jnz.lanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal.CtrlCBehaviour
import com.googlecode.lanterna.terminal.ansi.UnixTerminal
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import scala.util.Try

/** A Unix terminal that reads its size with `stty size` instead of Lanterna's cursor-position query, which blocks for good in a terminal that does not answer it. Ctrl+C reaches the app as a key. */
final class SttyTerminal(in: InputStream, out: OutputStream)
    extends UnixTerminal(in, out, Charset.defaultCharset, CtrlCBehaviour.TRAP):

  override protected def findTerminalSize(): TerminalSize =
    SttyTerminal.query().getOrElse(SttyTerminal.fallback)

object SttyTerminal:

  /** The terminal of the process, so that sbt's piping of the forked app's stdin and stdout is bypassed; the standard streams when there is no terminal. */
  def apply(): SttyTerminal =
    Try((FileInputStream("/dev/tty"), FileOutputStream("/dev/tty"))).toOption match
      case Some((in, out)) => new SttyTerminal(in, out)
      case None            => new SttyTerminal(System.in, System.out)

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
