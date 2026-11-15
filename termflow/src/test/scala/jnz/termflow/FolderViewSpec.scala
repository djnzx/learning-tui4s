package jnz.termflow

import java.nio.file.Paths
import jnz.Entry
import jnz.FolderModel
import jnz.State
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import termflow.tui.*

class FolderViewSpec extends AnyFlatSpec with Matchers:

  private val state = State(Paths.get("/tmp/demo"), Vector(Entry("..", isDir = true, size = 0), Entry("a.txt", isDir = false, size = 1234)), 0, 0, "")

  "view" should "put a text node on every row of the terminal" in {
    FolderView.view(state, 40, 6).children should have size 6
  }

  it should "draw the path in bold on the first row and the footer plain on the last" in {
    val nodes = FolderView.view(state, 40, 6).children.collect { case n: VNode.TextNode => n }
    nodes.head.txt.map(_.txt).mkString.trim shouldBe "/tmp/demo"
    nodes.head.txt.forall(_.style.bold) shouldBe true
    nodes.last.txt.map(_.txt).mkString.trim shouldBe FolderModel.footerKeys
    nodes.last.txt.exists(_.style.bold) shouldBe false
  }
