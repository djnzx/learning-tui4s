package jnz.lanterna

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal
import java.nio.file.Paths
import jnz.Entry
import jnz.FolderModel
import jnz.State
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
