package jnz

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

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

  def main(args: Array[String]): Unit =
    val _ = args
