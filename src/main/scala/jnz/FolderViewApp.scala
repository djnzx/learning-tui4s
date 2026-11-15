package jnz

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kyo.*
import kyo.UI.*
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.Using
import tui4s.*

/** A file or a folder of the shown folder. `..` is the parent folder. */
final case class Entry(name: String, isDir: Boolean, size: Long)

/** The shown folder, its entries, the selected entry and the first visible entry. The message is an error of the last step. */
final case class State(dir: Path, entries: Chunk[Entry], selected: Int, top: Int, message: String)

object FolderViewApp extends KyoApp:

  private val footerKeys = "Enter open   Backspace up   F10/q quit"

  run {
    Tui.run(TuiOptions()) { _ =>
      for
        size  <- Tui.size
        now   <- size.current
        start <- Sync.defer(Paths.get("").toAbsolutePath.normalize)
        first <- Sync.defer(visit(State(start, Chunk.empty, 0, 0, ""), start, None, rows(now)))
        state <- Signal.initRef(first)
      yield div.tabIndex(0)
        .onKeyDown(keys(state, size))
        .apply(
          state.combineLatest(size).render { case (st, sz) => screen(st, sz) },
        )
    }
  }

  /** Lines of the list: the terminal height without the header and the footer. */
  private def rows(size: TerminalSize): Int = math.max(1, size.height - 2)

  private def keys(state: SignalRef[State], size: Signal[TerminalSize])(e: KeyboardEvent): Any < Async =
    for
      now <- size.current
      st  <- state.current
      page = rows(now)
      _   <- e.key match
               case Keyboard.ArrowUp | Keyboard.Char('k')   => state.set(moveTo(st, st.selected - 1, page))
               case Keyboard.ArrowDown | Keyboard.Char('j') => state.set(moveTo(st, st.selected + 1, page))
               case Keyboard.PageUp                         => state.set(moveTo(st, st.selected - page, page))
               case Keyboard.PageDown                       => state.set(moveTo(st, st.selected + page, page))
               case Keyboard.Home                           => state.set(moveTo(st, 0, page))
               case Keyboard.End                            => state.set(moveTo(st, st.entries.size - 1, page))
               case Keyboard.Enter | Keyboard.ArrowRight    => enter(st, page).map(state.set)
               case Keyboard.Backspace | Keyboard.ArrowLeft => up(st, page).map(state.set)
               case Keyboard.F10 | Keyboard.Char('q')       => Tui.quit
               case _                                       => Kyo.unit
    yield ()

  private def moveTo(st: State, index: Int, page: Int): State =
    val sel = (index max 0) min ((st.entries.size - 1) max 0)
    st.copy(selected = sel, top = scroll(sel, st.top, page))

  /** The first visible entry that keeps `sel` in the window of `page` entries, starting from `top` when possible. */
  private def scroll(sel: Int, top: Int, page: Int): Int =
    if sel < top then sel
    else if sel >= top + page then sel - page + 1
    else top

  private def selected(st: State): Option[Entry] =
    Option.when(st.selected >= 0 && st.selected < st.entries.size)(st.entries(st.selected))

  private def enter(st: State, page: Int): State < Sync =
    selected(st) match
      case Some(e) if e.name == ".." => up(st, page)
      case Some(e) if e.isDir        => Sync.defer(visit(st, st.dir.resolve(e.name), None, page))
      case _                         => st

  /** Goes to the parent folder and selects the folder that was left. */
  private def up(st: State, page: Int): State < Sync =
    Option(st.dir.getParent) match
      case Some(parent) => Sync.defer(visit(st, parent, Option(st.dir.getFileName).map(_.toString), page))
      case None         => st

  /** Shows `dir` with `select` selected, or keeps `prev` with the error when `dir` cannot be read. */
  private def visit(prev: State, dir: Path, select: Option[String], page: Int): State =
    listing(dir) match
      case Left(error)    => prev.copy(message = error)
      case Right(entries) =>
        val index = math.max(0, select.map(name => entries.indexWhere(_.name == name)).getOrElse(0))
        State(dir, entries, index, scroll(index, 0, page), "")

  /** The parent entry (if any), then the folders and then the files, each group sorted by name. */
  private def listing(dir: Path): Either[String, Chunk[Entry]] =
    Try(Using.resource(Files.list(dir))(_.iterator.asScala.toVector)).toEither.left
      .map(_.toString)
      .map { paths =>
        val children = paths.map { p =>
          val isDir = Files.isDirectory(p)
          Entry(p.getFileName.toString, isDir, if isDir then 0L else Try(Files.size(p)).getOrElse(0L))
        }
        val up = Option(dir.getParent).map(_ => Entry("..", isDir = true, size = 0L)).toVector
        Chunk.from(up ++ children.sortBy(e => (!e.isDir, e.name.toLowerCase)))
      }

  private def screen(st: State, size: TerminalSize): UI =
    val page = rows(size)
    val width = size.width
    val top = scroll(st.selected, st.top, page)
    val listed = (0 until page).map { i =>
      val index = top + i
      if index < st.entries.size then
        val e = st.entries(index)
        val on = index == st.selected
        val text = line(if on then ">" else " ", e.name, sizeOf(e), width)
        val style = if on then Style.empty.bold else Style.empty
        div(span(text).style(style.whiteSpace(_.pre)))
      else div(span(""))
    }
    val footer = if st.message.nonEmpty then st.message else footerKeys
    div(
      div(span(cut(st.dir.toString, width)).style(Style.empty.bold)),
      UI.fragment(listed*),
      div(span(cut(footer, width))),
    )

  private def sizeOf(e: Entry): String =
    if e.name == ".." then "UP--DIR"
    else if e.isDir then "<DIR>"
    else e.size.toString

  /** One row: the marker and the name on the left, the size on the right, `width` cells in all. */
  private def line(mark: String, name: String, size: String, width: Int): String =
    val sizeWidth = 12
    val left = fit(s"$mark $name", math.max(0, width - sizeWidth))
    cut(left + String.format(s"%${sizeWidth}s", size.takeRight(sizeWidth)), width)

  /** The cells a code point takes on the terminal: a wide character (CJK, emoji) takes two, the others one. */
  private def cells(cp: Int): Int =
    val wide =
      (0x1100 to 0x115f).contains(cp) || (0x2e80 to 0xa4cf).contains(cp) || (0xac00 to 0xd7a3).contains(cp) ||
        (0xf900 to 0xfaff).contains(cp) || (0xfe30 to 0xfe4f).contains(cp) || (0xff00 to 0xff60).contains(cp) ||
        (0xffe0 to 0xffe6).contains(cp) || (0x1f300 to 0x1f64f).contains(cp) || (0x1f680 to 0x1f6ff).contains(cp) ||
        (0x1f900 to 0x1f9ff).contains(cp) || (0x20000 to 0x3fffd).contains(cp)
    if wide then 2 else 1

  /** The longest start of `text` that takes at most `width` cells. */
  private def cut(text: String, width: Int): String =
    val cps = text.codePoints.toArray
    val used = cps.scanLeft(0)(_ + cells(_)).tail
    new String(cps, 0, used.takeWhile(_ <= width).length)

  /** `text` cut to `width` cells and padded with spaces to take exactly `width` cells. */
  private def fit(text: String, width: Int): String =
    val kept = cut(text, width)
    kept + " " * math.max(0, width - kept.codePoints.toArray.map(cells).sum)
