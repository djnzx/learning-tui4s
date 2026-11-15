package jnz.termflow

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer
import java.nio.charset.StandardCharsets
import jnz.Stty
import scala.jdk.CollectionConverters.*
import sun.misc.Signal
import sun.misc.SignalHandler
import termflow.tui.Capabilities
import termflow.tui.TerminalBackend

/**
 * TermFlow's terminal on `/dev/tty`: the keys are read and the screen is written there, and the size comes from `stty`.
 * TermFlow's own JLine backend takes the terminal from stdin and stdout, which are pipes when sbt starts the app forked,
 * and falls back to a dumb terminal that shows nothing.
 */
final class TtyTerminalBackend extends TerminalBackend:
  private val saved = Stty.save()
  private val in    = FileInputStream("/dev/tty")
  private val out   = OutputStreamWriter(FileOutputStream("/dev/tty"), StandardCharsets.UTF_8)
  Stty.raw()

  @volatile private var size                       = Stty.size().getOrElse((80, 24))
  @volatile private var resized: Option[() => Unit] = None

  /** TermFlow closes the reader when the app ends, while its key thread is blocked in `read`; the close of an `InputStreamReader` would wait for the next key. */
  private val inReader: Reader = new Reader:
    private val inner = InputStreamReader(in, StandardCharsets.UTF_8)

    override def read(chars: Array[Char], offset: Int, length: Int): Int = inner.read(chars, offset, length)
    override def ready(): Boolean                                       = inner.ready()
    override def close(): Unit                                          = ()

  private val previous = Signal.handle(
    Signal("WINCH"),
    new SignalHandler:
      override def handle(signal: Signal): Unit =
        size = Stty.size().getOrElse(size)
        resized.foreach(_())
  )

  override def reader: Reader = inReader
  override def writer: Writer = out
  override def width: Int     = size._1
  override def height: Int    = size._2

  override val capabilities: Capabilities = Capabilities.detect(System.getenv().asScala.toMap)

  override def onResize(listener: () => Unit): Option[() => Unit] =
    resized = Some(listener)
    Some(() => resized = None)

  override def close(): Unit =
    val _ = Signal.handle(Signal("WINCH"), previous)
    out.flush()
    saved.foreach(Stty.restore)
