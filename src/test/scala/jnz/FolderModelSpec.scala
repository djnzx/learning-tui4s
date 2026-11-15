package jnz

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FolderModelSpec extends AnyFlatSpec with Matchers:

  private def sampleDir(): Path =
    val d = Files.createTempDirectory("fv")
    Files.write(d.resolve("b.txt"), "12345".getBytes)
    Files.write(d.resolve("A.txt"), Array.emptyByteArray)
    Files.createDirectory(d.resolve("z"))
    Files.createDirectory(d.resolve("c"))
    d

  private def entries(n: Int): Vector[Entry] = Vector.tabulate(n)(i => Entry(s"f$i", isDir = false, size = i.toLong))

  private def stateOf(n: Int, selected: Int = 0, top: Int = 0): State =
    State(Paths.get("/tmp"), entries(n), selected, top, "")

  private def width(s: String): Int = s.codePoints.toArray.map(FolderModel.cells).sum

  "listing" should "put .., folders, then files, each sorted case-insensitively" in {
    val Right(es) = FolderModel.listing(sampleDir()): @unchecked
    es.map(_.name) shouldBe Vector("..", "c", "z", "A.txt", "b.txt")
    es.find(_.name == "b.txt").map(_.size) shouldBe Some(5L)
  }

  it should "have no .. at the filesystem root" in {
    val Right(es) = FolderModel.listing(Paths.get("/")): @unchecked
    es.map(_.name) should not contain ".."
  }

  it should "fail for a missing path" in {
    FolderModel.listing(Paths.get("/no/such/dir/at/all")).isLeft shouldBe true
  }

  "apply" should "move by one line and clamp at both ends" in {
    FolderModel(stateOf(10), Action.LineDown, 3).selected shouldBe 1
    FolderModel(stateOf(10, selected = 9, top = 7), Action.LineDown, 3).selected shouldBe 9
    FolderModel(stateOf(10), Action.LineUp, 3).selected shouldBe 0
  }

  it should "move by a page and keep the selection visible" in {
    val st = FolderModel(stateOf(10), Action.PageDown, 3)
    st.selected shouldBe 3
    st.selected should be < (st.top + 3)
    FolderModel(st, Action.PageUp, 3).selected shouldBe 0
  }

  it should "jump to the first and the last entry" in {
    FolderModel(stateOf(10, 5, 3), Action.Home, 3).selected shouldBe 0
    val end = FolderModel(stateOf(10), Action.End, 3)
    end.selected shouldBe 9
    end.top shouldBe 7
  }

  it should "open a folder and select its first entry" in {
    val d  = sampleDir()
    val st = FolderModel.initial(d, 10)
    val c  = st.copy(selected = st.entries.indexWhere(_.name == "c"))
    val in = FolderModel(c, Action.Open, 10)
    in.dir shouldBe d.resolve("c")
    in.selected shouldBe 0
  }

  it should "go to the parent and select the folder that was left" in {
    val d      = sampleDir()
    val inside = FolderModel.initial(d.resolve("z"), 10)
    val out    = FolderModel(inside, Action.Parent, 10)
    out.dir shouldBe d
    out.entries(out.selected).name shouldBe "z"
    FolderModel(inside.copy(selected = 0), Action.Open, 10).dir shouldBe d
  }

  it should "stay at the filesystem root on Parent" in {
    val root = FolderModel.initial(Paths.get("/"), 10)
    FolderModel(root, Action.Parent, 10) shouldBe root
  }

  it should "keep the previous state with a message when the folder cannot be read, and clear it later" in {
    val prev   = stateOf(3)
    val failed = FolderModel.visit(prev, Paths.get("/no/such/dir/at/all"), None, 3)
    failed.dir shouldBe prev.dir
    failed.entries shouldBe prev.entries
    failed.message should not be empty
    FolderModel(failed, Action.LineDown, 3).message shouldBe ""
  }

  it should "report a folder without read permission and keep the previous state" in {
    val d      = sampleDir()
    val locked = d.resolve("c")
    val prev   = FolderModel.initial(d, 10)
    Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"))
    try
      assume(!Files.isReadable(locked), "running with rights that ignore permissions (root)")
      val failed = FolderModel.visit(prev, locked, None, 10)
      failed.dir shouldBe prev.dir
      failed.entries shouldBe prev.entries
      failed.message should include("AccessDenied")
    finally Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("rwx------"))
  }

  it should "clear the message when Open or Parent changes nothing" in {
    val onFile = FolderModel.initial(sampleDir(), 10)
    val file   = onFile.copy(selected = onFile.entries.indexWhere(_.name == "b.txt"), message = "boom")
    FolderModel(file, Action.Open, 10).message shouldBe ""
    val root = FolderModel.initial(Paths.get("/"), 10).copy(message = "boom")
    FolderModel(root, Action.Parent, 10).message shouldBe ""
  }

  it should "survive an empty entry list" in {
    val empty = State(Paths.get("/tmp"), Vector.empty, 0, 0, "")
    Action.values.filterNot(_ == Action.Open).filterNot(_ == Action.Parent).foreach { a =>
      FolderModel(empty, a, 3).selected shouldBe 0
    }
    FolderModel(empty, Action.Open, 3).selected shouldBe 0
  }

  "cell helpers" should "cut and fit by terminal cells" in {
    FolderModel.cut("日本語abc", 5) shouldBe "日本"
    FolderModel.fit("日本", 5) shouldBe "日本 "
    width(FolderModel.fit("日本", 5)) shouldBe 5
  }

  "line" should "be exactly the given width, also for long and wide names" in {
    width(FolderModel.line(">", "x" * 80, "12", 20)) shouldBe 20
    width(FolderModel.line(" ", "日本語" * 10, "<DIR>", 30)) shouldBe 30
  }

  it should "not fail when narrower than the size column" in {
    width(FolderModel.line(">", "name", "12", 5)) should be <= 5
  }

  "sizeOf" should "describe .., folders and files" in {
    FolderModel.sizeOf(Entry("..", isDir = true, size = 0)) shouldBe "UP--DIR"
    FolderModel.sizeOf(Entry("d", isDir = true, size = 0)) shouldBe "<DIR>"
    FolderModel.sizeOf(Entry("f", isDir = false, size = 1234)) shouldBe "1234"
  }
