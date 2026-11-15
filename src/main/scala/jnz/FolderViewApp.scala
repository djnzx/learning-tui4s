package jnz

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.nio.file.Paths
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

  private val pollMillis = 20L

  /** Resizes, draws and polls for a key until a quit action; the state changes only through `FolderModel`. */
  @tailrec
  private def loop(screen: Screen, state: State): Unit =
    val _ = screen.doResizeIfNecessary()
    FolderView.draw(screen, state)
    screen.pollInput() match
      case null                                   =>
        Thread.sleep(pollMillis)
        loop(screen, state)
      case key                                    =>
        toAction(key) match
          case Some(Action.Quit) => ()
          case Some(action)      => loop(screen, FolderModel(state, action, FolderModel.rows(screen.getTerminalSize.getRows)))
          case None              => loop(screen, state)

  def main(args: Array[String]): Unit =
    val _      = args
    val screen = DefaultTerminalFactory().createScreen()
    screen.startScreen()
    try
      val start = Paths.get("").toAbsolutePath.normalize
      loop(screen, FolderModel.initial(start, FolderModel.rows(screen.getTerminalSize.getRows)))
    finally screen.stopScreen()
