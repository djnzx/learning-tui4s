package jnz

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

  "view" should "put a text node on every row of the terminal" in {
    FolderView.view(state(), 40, 6).children should have size 6
  }
