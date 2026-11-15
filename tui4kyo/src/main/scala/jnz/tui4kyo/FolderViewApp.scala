package jnz.tui4kyo

import java.nio.file.Path
import java.nio.file.Paths
import jnz.Action
import jnz.FolderLines
import jnz.FolderModel
import jnz.State
import kyo.*
import kyo.UI.*
import tui4s.*

/** The folder browser on tui4s and Kyo: the state is a `SignalRef` of the `core` state, the screen follows it and the size of the terminal. */
object FolderViewApp extends KyoApp:

  run {
    Tui.run(TuiOptions()) { _ =>
      Sync.defer(Paths.get("").toAbsolutePath.normalize).map(ui)
    }
  }

  /** The UI of the folder `start`. Quit is `Tui.quit`, which works inside a handler, also in the headless run of the tests. */
  def ui(start: Path): UI < (Async & Scope) =
    for
      size  <- Tui.size
      now   <- size.current
      first <- Sync.defer(FolderModel.initial(start, FolderModel.rows(now.height)))
      state <- Signal.initRef(first)
    yield div.tabIndex(0)
      .onKeyDown(keys(state, size))
      .apply(
        state.combineLatest(size).render { case (st, sz) => screen(st, sz) },
      )

  def toAction(key: Keyboard): Option[Action] =
    key match
      case Keyboard.ArrowUp | Keyboard.Char('k')   => Some(Action.LineUp)
      case Keyboard.ArrowDown | Keyboard.Char('j') => Some(Action.LineDown)
      case Keyboard.PageUp                         => Some(Action.PageUp)
      case Keyboard.PageDown                       => Some(Action.PageDown)
      case Keyboard.Home                           => Some(Action.Home)
      case Keyboard.End                            => Some(Action.End)
      case Keyboard.Enter | Keyboard.ArrowRight    => Some(Action.Open)
      case Keyboard.Backspace | Keyboard.ArrowLeft => Some(Action.Parent)
      case Keyboard.F10 | Keyboard.Char('q')       => Some(Action.Quit)
      case _                                       => None

  private def keys(state: SignalRef[State], size: Signal[TerminalSize])(e: KeyboardEvent): Any < Async =
    for
      now <- size.current
      st  <- state.current
      _   <- toAction(e.key) match
               case Some(Action.Quit) => Tui.quit
               case Some(action)      => Sync.defer(FolderModel(st, action, FolderModel.rows(now.height))).map(state.set)
               case None              => Kyo.unit
    yield ()

  /** The rows of `FolderLines`: the header (bold) and the footer as they are, the list rows with their spaces kept (the selected one bold). */
  private def screen(st: State, size: TerminalSize): UI =
    val rows = FolderLines.lines(st, size.width, size.height)
    val last = rows.size - 1
    UI.fragment(rows.zipWithIndex.map { case ((text, bold), i) =>
      val style = if bold then Style.empty.bold else Style.empty
      val inList = i > 0 && i < last
      div(span(text).style(if inList then style.whiteSpace(_.pre) else style))
    }*)
