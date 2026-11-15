package jnz.lanterna

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import java.nio.file.Paths
import jnz.Action
import jnz.FolderModel
import jnz.State
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec

/** The folder browser on a Lanterna screen. */
object FolderViewApp:

  def toAction(key: KeyStroke): Option[Action] =
    key.getKeyType match
      case KeyType.ArrowUp                       => Some(Action.LineUp)
      case KeyType.ArrowDown                     => Some(Action.LineDown)
      case KeyType.PageUp                        => Some(Action.PageUp)
      case KeyType.PageDown                      => Some(Action.PageDown)
      case KeyType.Home                          => Some(Action.Home)
      case KeyType.End                           => Some(Action.End)
      case KeyType.Enter | KeyType.ArrowRight    => Some(Action.Open)
      case KeyType.Backspace | KeyType.ArrowLeft => Some(Action.Parent)
      case KeyType.F10 | KeyType.EOF             => Some(Action.Quit)
      case KeyType.Character                     =>
        key.getCharacter.charValue match
          case 'k'                    => Some(Action.LineUp)
          case 'j'                    => Some(Action.LineDown)
          case 'q'                    => Some(Action.Quit)
          case 'c' if key.isCtrlDown  => Some(Action.Quit)
          case _                      => None
      case _                                     => None

  private val pollMillis = 50L

  /** Draws only when the state changed or the terminal was resized, then polls for a key until a quit action. */
  @tailrec
  private def loop(screen: Screen, resized: AtomicBoolean, state: State, dirty: Boolean): Unit =
    val redraw = resized.getAndSet(false)
    if redraw then { val _ = screen.doResizeIfNecessary() }
    if dirty || redraw then FolderView.draw(screen, state)
    screen.pollInput() match
      case null =>
        Thread.sleep(pollMillis)
        loop(screen, resized, state, dirty = false)
      case key  =>
        toAction(key) match
          case Some(Action.Quit) => ()
          case Some(action)      =>
            loop(screen, resized, FolderModel(state, action, FolderModel.rows(screen.getTerminalSize.getRows)), dirty = true)
          case None              => loop(screen, resized, state, dirty = false)

  def main(args: Array[String]): Unit =
    val _        = args
    val terminal = SttyTerminal()
    val screen   = TerminalScreen(terminal)
    val resized  = AtomicBoolean(false)
    terminal.addResizeListener((_, _) => resized.set(true))
    screen.startScreen()
    try
      val start = Paths.get("").toAbsolutePath.normalize
      loop(screen, resized, FolderModel.initial(start, FolderModel.rows(screen.getTerminalSize.getRows)), dirty = true)
    finally screen.stopScreen()
