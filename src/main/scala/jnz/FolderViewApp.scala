package jnz

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import fs2.Stream
import jatatui.core.terminal.Terminal
import jatatui.crossterm.CrosstermBackend
import jatatui.crossterm.Jatatui
import java.nio.file.Paths
import tui.crossterm.CrosstermJni
import tui.crossterm.Event
import tui.crossterm.KeyCode
import tui.crossterm.KeyEventKind

object FolderViewApp extends IOApp.Simple:

  type Term = Terminal[CrosstermBackend]

  /** The terminal in raw mode on the alternate screen; restored when the resource is released, whatever the outcome. */
  private val terminal: Resource[IO, Term] =
    Resource.make(IO.blocking(Jatatui.init()))(t => IO.blocking(t.close()).attempt *> IO.blocking(Jatatui.restore()))

  def toAction(event: Event): Option[Action] =
    event match
      case k: Event.Key if k.keyEvent.kind == KeyEventKind.Press =>
        k.keyEvent.code match
          case _: KeyCode.Up                              => Some(Action.LineUp)
          case c: KeyCode.Char if c.c == 'k'              => Some(Action.LineUp)
          case _: KeyCode.Down                            => Some(Action.LineDown)
          case c: KeyCode.Char if c.c == 'j'              => Some(Action.LineDown)
          case _: KeyCode.PageUp                          => Some(Action.PageUp)
          case _: KeyCode.PageDown                        => Some(Action.PageDown)
          case _: KeyCode.Home                            => Some(Action.Home)
          case _: KeyCode.End                             => Some(Action.End)
          case _: KeyCode.Enter | _: KeyCode.Right        => Some(Action.Open)
          case _: KeyCode.Backspace | _: KeyCode.Left     => Some(Action.Parent)
          case f: KeyCode.F if f.num == 10                => Some(Action.Quit)
          case c: KeyCode.Char if c.c == 'q'              => Some(Action.Quit)
          case _                                          => None
      case _ => None

  def run: IO[Unit] =
    terminal.use { term =>
      val ct = new CrosstermJni()
      val pageNow = IO.blocking(FolderModel.rows(term.size().height()))
      for
        start <- IO.blocking(Paths.get("").toAbsolutePath.normalize)
        page  <- pageNow
        state <- IO.ref(FolderModel.initial(start, page))
        draw   = state.get.flatMap(st => IO.blocking(term.draw(frame => FolderView.draw(frame, st))).void)
        _     <- draw
        _     <- events(ct)
                   .evalMap {
                     case _: Event.Resize => draw.as(true)
                     case e =>
                       toAction(e) match
                         case Some(Action.Quit) => IO.pure(false)
                         case Some(action)      =>
                           pageNow.flatMap(p => state.get.flatMap(st => IO.blocking(FolderModel(st, action, p))).flatMap(state.set)) *>
                             draw.as(true)
                         case None => IO.pure(true)
                   }
                   .takeWhile(identity)
                   .compile
                   .drain
      yield ()
    }

  /** Terminal events as they come; `read` blocks, so cancelling the stream interrupts the wait. */
  private def events(ct: CrosstermJni): Stream[IO, Event] =
    Stream.repeatEval(IO.interruptible(ct.read()))
