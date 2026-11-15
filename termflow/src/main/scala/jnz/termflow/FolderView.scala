package jnz.termflow

import jnz.FolderLines
import jnz.State
import termflow.tui.*
import termflow.tui.TuiPrelude.*

object FolderView:

  def view(st: State, width: Int, height: Int): RootNode =
    val nodes = FolderLines.lines(st, width, height).zipWithIndex.map { case ((text, bold), y) =>
      VNode.TextNode(1.x, (y + 1).y, List(Text(text, Style(bold = bold))))
    }
    RootNode(width, height, nodes.toList, None)
