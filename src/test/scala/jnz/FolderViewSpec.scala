package jnz

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal
import java.nio.file.Paths
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FolderViewSpec extends AnyFlatSpec with Matchers:

  private val entries = Vector(
    Entry("..", isDir = true, size = 0),
    Entry("docs", isDir = true, size = 0),
    Entry("a.txt", isDir = false, size = 1234),
    Entry("b.txt", isDir = false, size = 5),
    Entry("c.txt", isDir = false, size = 7),
  )

  private def state(selected: Int = 0, top: Int = 0, message: String = ""): State =
    State(Paths.get("/tmp/demo"), entries, selected, top, message)

  private def drawn(st: State, width: Int, height: Int): Vector[String] =
    FolderView.lines(st, width, height).map(_._1)

  "lines" should "show the path, the visible entries and the key hints" in {
    val rows = drawn(state(selected = 1), 40, 6)
    rows.head.trim shouldBe "/tmp/demo"
    rows(1) should startWith("  ..")
    rows(1) should endWith("     UP--DIR")
    rows(2) should startWith("> docs")
    rows(2) should endWith("       <DIR>")
    rows(3) should endWith("        1234")
    rows(4) should startWith("  b.txt")
    rows(5).trim shouldBe FolderModel.footerKeys
  }

  it should "scroll the list by top" in {
    val rows = drawn(state(selected = 4, top = 2), 40, 5)
    rows(1) should startWith("  a.txt")
    rows(3) should startWith("> c.txt")
  }

  it should "keep the selection visible after the terminal shrinks" in {
    val rows = drawn(state(selected = 4, top = 0), 40, 4)
    rows(2) should startWith("> c.txt")
  }

  it should "show the message instead of the key hints" in {
    drawn(state(message = "boom"), 40, 6).last.trim shouldBe "boom"
  }

  it should "not fail on a tiny terminal" in {
    drawn(state(), 40, 2) should have size 2
    drawn(state(), 40, 2).last.trim shouldBe FolderModel.footerKeys
    drawn(state(), 4, 6) should have size 6
    drawn(state(), 1, 1) should have size 1
  }

  it should "make the header and the selected entry bold" in {
    FolderView.lines(state(selected = 1), 40, 6).map(_._2) shouldBe Vector(true, false, true, false, false, false)
  }

  private def screenOf(width: Int, height: Int): Screen =
    val screen = TerminalScreen(DefaultVirtualTerminal(TerminalSize(width, height)))
    screen.startScreen()
    screen

  private def cell(screen: Screen, x: Int, y: Int) = screen.getFrontCharacter(TerminalPosition(x, y))

  private def rowText(screen: Screen, y: Int, width: Int): String =
    (0 until width).map(x => cell(screen, x, y).getCharacterString).mkString

  "draw" should "paint the path in bold on row 0 and the selected entry in bold" in {
    val screen = screenOf(40, 6)
    FolderView.draw(screen, state(selected = 1))
    cell(screen, 0, 0).getCharacterString shouldBe "/"
    cell(screen, 0, 0).getModifiers.contains(SGR.BOLD) shouldBe true
    cell(screen, 0, 1).getCharacterString shouldBe " "
    cell(screen, 0, 1).getModifiers.contains(SGR.BOLD) shouldBe false
    cell(screen, 0, 2).getCharacterString shouldBe ">"
    cell(screen, 0, 2).getModifiers.contains(SGR.BOLD) shouldBe true
  }

  it should "paint the footer on the last row" in {
    val screen = screenOf(40, 6)
    FolderView.draw(screen, state())
    rowText(screen, 5, 40).trim shouldBe FolderModel.footerKeys
  }

  it should "not fail on tiny terminals" in {
    noException should be thrownBy FolderView.draw(screenOf(1, 1), state())
    noException should be thrownBy FolderView.draw(screenOf(4, 2), state())
  }
