package jnz.lanterna

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.screen.Screen
import jnz.FolderLines
import jnz.State

object FolderView:

  /** Paints `lines` for the current size of the screen: the header and the selected entry in bold. */
  def draw(screen: Screen, st: State): Unit =
    val size = screen.getTerminalSize
    val tg   = screen.newTextGraphics()
    screen.clear()
    FolderLines.lines(st, size.getColumns, size.getRows).zipWithIndex.foreach { case ((text, bold), y) =>
      tg.clearModifiers()
      if bold then { val _ = tg.enableModifiers(SGR.BOLD) }
      tg.putString(0, y, text)
    }
    screen.refresh()
