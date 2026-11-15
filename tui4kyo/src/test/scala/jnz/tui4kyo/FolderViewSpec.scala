package jnz.tui4kyo

import java.nio.file.Files
import java.nio.file.Path
import jnz.FolderModel
import kyo.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tui4s.testkit.Headless

/** The app on tui4s's headless run: a screen of 100 x 8 cells, no terminal. */
class FolderViewSpec extends AnyFlatSpec with Matchers:

  import AllowUnsafe.embrace.danger

  private def run[A](v: A < (Async & Scope & Abort[Any])): A = KyoApp.Unsafe.runAndBlock(30.seconds)(v).getOrThrow

  /** A folder with the folder `docs` and the file `a.txt` of 4 bytes. */
  private def folder(): Path =
    val dir = Files.createTempDirectory("folderview").toRealPath()
    Files.createDirectory(dir.resolve("docs"))
    Files.writeString(dir.resolve("a.txt"), "1234")
    dir

  private def started(dir: Path) = Headless.start(100, 8)(_ => FolderViewApp.ui(dir))

  "the app" should "show the path, the entries and the key hints" in {
    val dir   = folder()
    val lines = run(started(dir).map(_.lines))
    lines should have size 8
    lines(0).trim shouldBe dir.toString
    lines(1) should startWith("> ..")
    lines(2) should startWith("  docs")
    lines(2).trim should endWith("<DIR>")
    lines(3) should startWith("  a.txt")
    lines(3).trim should endWith("4")
    lines(7).trim shouldBe FolderModel.footerKeys
  }

  it should "move the selection with j" in {
    val lines = run(started(folder()).flatMap(h => h.char('j').andThen(h.lines)))
    lines(1) should startWith("  ..")
    lines(2) should startWith("> docs")
  }

  it should "open the selected folder with Enter and go up with Backspace" in {
    val dir = folder()
    val (inside, back) = run(started(dir).flatMap(h =>
      for
        _      <- h.char('j')
        _      <- h.char('\n')
        inside <- h.lines
        _      <- h.key(kyo.UI.Keyboard.Backspace)
        back   <- h.lines
      yield (inside, back)
    ))
    inside(0).trim shouldBe dir.resolve("docs").toString
    inside(1) should startWith("> ..")
    back(0).trim shouldBe dir.toString
    back(2) should startWith("> docs")
  }

  it should "end the run on q" in {
    val ended = run(started(folder()).flatMap(h => h.char('q').andThen(h.awaitEnd(5.seconds))))
    ended shouldBe true
  }

  it should "keep the whole page of entries while scrolling a big folder" in {
    val dir = Files.createTempDirectory("folderview-big").toRealPath()
    (0 until 60).foreach(i => Files.writeString(dir.resolve(f"file$i%02d.txt"), "x"))
    val screens = run(Headless.start(100, 12)(_ => FolderViewApp.ui(dir)).flatMap { h =>
      val keys = Vector.fill(12)(kyo.UI.Keyboard.ArrowDown) ++ Vector(kyo.UI.Keyboard.PageDown, kyo.UI.Keyboard.End, kyo.UI.Keyboard.ArrowUp, kyo.UI.Keyboard.PageUp, kyo.UI.Keyboard.Home)
      Kyo.foreach(keys)(k => h.key(k).andThen(h.lines))
    })
    screens.foreach(_.slice(1, 11).count(_.trim.nonEmpty) shouldBe 10)
  }
