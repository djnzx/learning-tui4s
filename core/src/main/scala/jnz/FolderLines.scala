package jnz

/** The text of the screen, independent of the terminal library. */
object FolderLines:

  /** The `height` rows of the screen, each `width` cells wide: the header (the path), the list window and the footer (the message or the key hints). Bold marks the header and the selected entry. */
  def lines(st: State, width: Int, height: Int): Vector[(String, Boolean)] =
    val page   = FolderModel.rows(height)
    val top    = FolderModel.scroll(st.selected, st.top, page)
    val window = st.entries.slice(top, top + page)
    val items  = window.zipWithIndex.map { (e, i) =>
      val selected = top + i == st.selected
      (FolderModel.line(if selected then ">" else " ", e.name, FolderModel.sizeOf(e), width), selected)
    }
    val footer = if st.message.nonEmpty then st.message else FolderModel.footerKeys
    val blank  = (FolderModel.fit("", width), false)
    val body   = if height > 2 then (items ++ Vector.fill(page - items.size)(blank)).take(height - 2) else Vector.empty
    val head   = Vector((FolderModel.fit(st.dir.toString, width), true))
    val foot   = if height > 1 then Vector((FolderModel.fit(footer, width), false)) else Vector.empty
    (head ++ body ++ foot).take(height)
