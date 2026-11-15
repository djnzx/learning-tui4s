package jnz.jatatui

import jatatui.core.buffer.Buffer
import jatatui.core.layout.Rect
import jatatui.core.style.Style
import jatatui.core.terminal.Frame
import jatatui.widgets.list.List as ListWidget
import jatatui.widgets.list.ListItem
import jatatui.widgets.list.ListState
import jatatui.widgets.paragraph.Paragraph
import java.util.Optional
import jnz.FolderModel
import jnz.State
import scala.jdk.CollectionConverters.*

object FolderView:

  def draw(frame: Frame, st: State): Unit =
    render(st, frame.area(), frame.bufferMut())

  /** The header (the path), the list window and the footer (the message or the key hints) in `area`. */
  def render(st: State, area: Rect, buf: Buffer): Unit =
    val width  = area.width()
    val page   = FolderModel.rows(area.height())
    val top    = FolderModel.scroll(st.selected, st.top, page)
    val window = st.entries.slice(top, top + page)
    val items  = window.zipWithIndex.map { (e, i) =>
      val mark = if top + i == st.selected then ">" else " "
      ListItem.of(FolderModel.line(mark, e.name, FolderModel.sizeOf(e), width))
    }
    val chosen = Optional.of(Integer.valueOf(st.selected - top)).filter(_ => window.nonEmpty)
    val footer = if st.message.nonEmpty then st.message else FolderModel.footerKeys

    Paragraph
      .of(FolderModel.cut(st.dir.toString, width))
      .withStyle(Style.empty().bold())
      .render(Rect.of(area.x(), area.y(), width, math.min(1, area.height())), buf)
    if area.height() > 2 then
      val list = ListWidget.of(items.asJava).withHighlightStyle(Style.empty().bold())
      list.render(Rect.of(area.x(), area.y() + 1, width, area.height() - 2), buf, ListState.empty().withSelected(chosen))
    if area.height() > 1 then
      Paragraph
        .of(FolderModel.cut(footer, width))
        .render(Rect.of(area.x(), area.y() + area.height() - 1, width, 1), buf)
