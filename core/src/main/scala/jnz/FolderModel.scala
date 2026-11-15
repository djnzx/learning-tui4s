package jnz

import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.Using

/** A file or a folder of the shown folder. `..` is the parent folder. */
final case class Entry(name: String, isDir: Boolean, size: Long)

/** The shown folder, its entries, the selected entry and the first visible entry. The message is an error of the last step. */
final case class State(dir: Path, entries: Vector[Entry], selected: Int, top: Int, message: String)

enum Action:
  case LineUp, LineDown, PageUp, PageDown, Home, End, Open, Parent, Quit

object FolderModel:

  val footerKeys = "Enter open   Backspace up   F10/q quit"

  private val sizeWidth = 12

  /** Lines of the list: the terminal height without the header and the footer. */
  def rows(height: Int): Int = math.max(1, height - 2)

  /** The parent entry (if any), then the folders and then the files, each group sorted by name. */
  def listing(dir: Path): Either[String, Vector[Entry]] =
    Try(Using.resource(Files.list(dir))(_.iterator.asScala.toVector)).toEither.left
      .map(_.toString)
      .map { paths =>
        val children = paths.map { p =>
          val isDir = Files.isDirectory(p)
          Entry(p.getFileName.toString, isDir, if isDir then 0L else Try(Files.size(p)).getOrElse(0L))
        }
        val up = Option(dir.getParent).map(_ => Entry("..", isDir = true, size = 0L)).toVector
        up ++ children.sortBy(e => (!e.isDir, e.name.toLowerCase))
      }

  /** Shows `dir` with `select` selected, or keeps `prev` with the error when `dir` cannot be read. */
  def visit(prev: State, dir: Path, select: Option[String], page: Int): State =
    listing(dir) match
      case Left(error)    => prev.copy(message = error)
      case Right(entries) =>
        val index = math.max(0, select.map(name => entries.indexWhere(_.name == name)).getOrElse(0))
        State(dir, entries, index, scroll(index, 0, page), "")

  def initial(dir: Path, page: Int): State =
    visit(State(dir, Vector.empty, 0, 0, ""), dir, None, page)

  def moveTo(st: State, index: Int, page: Int): State =
    val sel = (index max 0) min ((st.entries.size - 1) max 0)
    st.copy(selected = sel, top = scroll(sel, st.top, page), message = "")

  /** The first visible entry that keeps `sel` in the window of `page` entries, starting from `top` when possible. */
  def scroll(sel: Int, top: Int, page: Int): Int =
    if sel < top then sel
    else if sel >= top + page then sel - page + 1
    else top

  def apply(st: State, action: Action, page: Int): State =
    action match
      case Action.LineUp   => moveTo(st, st.selected - 1, page)
      case Action.LineDown => moveTo(st, st.selected + 1, page)
      case Action.PageUp   => moveTo(st, st.selected - page, page)
      case Action.PageDown => moveTo(st, st.selected + page, page)
      case Action.Home     => moveTo(st, 0, page)
      case Action.End      => moveTo(st, st.entries.size - 1, page)
      case Action.Open     => open(st, page)
      case Action.Parent   => up(st, page)
      case Action.Quit     => st

  private def selectedEntry(st: State): Option[Entry] =
    Option.when(st.selected >= 0 && st.selected < st.entries.size)(st.entries(st.selected))

  private def open(st: State, page: Int): State =
    selectedEntry(st) match
      case Some(e) if e.name == ".." => up(st, page)
      case Some(e) if e.isDir        => visit(st, st.dir.resolve(e.name), None, page)
      case _                         => st.copy(message = "")

  /** Goes to the parent folder and selects the folder that was left. */
  private def up(st: State, page: Int): State =
    Option(st.dir.getParent) match
      case Some(parent) => visit(st, parent, Option(st.dir.getFileName).map(_.toString), page)
      case None         => st.copy(message = "")

  def sizeOf(e: Entry): String =
    if e.name == ".." then "UP--DIR"
    else if e.isDir then "<DIR>"
    else e.size.toString

  /** One row: the marker and the name on the left, the size on the right, `width` cells in all. */
  def line(mark: String, name: String, size: String, width: Int): String =
    val left = fit(s"$mark $name", math.max(0, width - sizeWidth))
    cut(left + String.format(s"%${sizeWidth}s", size.takeRight(sizeWidth)), width)

  /** The cells a code point takes on the terminal: a wide character (CJK, emoji) takes two, the others one. */
  def cells(cp: Int): Int =
    val wide =
      (0x1100 to 0x115f).contains(cp) || (0x2e80 to 0xa4cf).contains(cp) || (0xac00 to 0xd7a3).contains(cp) ||
        (0xf900 to 0xfaff).contains(cp) || (0xfe30 to 0xfe4f).contains(cp) || (0xff00 to 0xff60).contains(cp) ||
        (0xffe0 to 0xffe6).contains(cp) || (0x1f300 to 0x1f64f).contains(cp) || (0x1f680 to 0x1f6ff).contains(cp) ||
        (0x1f900 to 0x1f9ff).contains(cp) || (0x20000 to 0x3fffd).contains(cp)
    if wide then 2 else 1

  /** The longest start of `text` that takes at most `width` cells. */
  def cut(text: String, width: Int): String =
    val cps  = text.codePoints.toArray
    val used = cps.scanLeft(0)(_ + cells(_)).tail
    new String(cps, 0, used.takeWhile(_ <= width).length)

  /** `text` cut to `width` cells and padded with spaces to take exactly `width` cells. */
  def fit(text: String, width: Int): String =
    val kept = cut(text, width)
    kept + " " * math.max(0, width - kept.codePoints.toArray.map(cells).sum)
