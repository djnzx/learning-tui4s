package jnz

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tui.crossterm.Event
import tui.crossterm.KeyCode
import tui.crossterm.KeyEvent
import tui.crossterm.KeyEventKind

class KeysSpec extends AnyFlatSpec with Matchers:

  private def key(code: KeyCode, kind: KeyEventKind = KeyEventKind.Press): Event =
    Event.Key(new KeyEvent(code, null, kind, null))

  "toAction" should "map the keys of the spec" in {
    FolderViewApp.toAction(key(new KeyCode.Up())) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(key(new KeyCode.Char('k'))) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(key(new KeyCode.Down())) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(key(new KeyCode.Char('j'))) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(key(new KeyCode.PageUp())) shouldBe Some(Action.PageUp)
    FolderViewApp.toAction(key(new KeyCode.PageDown())) shouldBe Some(Action.PageDown)
    FolderViewApp.toAction(key(new KeyCode.Home())) shouldBe Some(Action.Home)
    FolderViewApp.toAction(key(new KeyCode.End())) shouldBe Some(Action.End)
    FolderViewApp.toAction(key(new KeyCode.Enter())) shouldBe Some(Action.Open)
    FolderViewApp.toAction(key(new KeyCode.Right())) shouldBe Some(Action.Open)
    FolderViewApp.toAction(key(new KeyCode.Backspace())) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(key(new KeyCode.Left())) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(key(new KeyCode.F(10))) shouldBe Some(Action.Quit)
    FolderViewApp.toAction(key(new KeyCode.Char('q'))) shouldBe Some(Action.Quit)
  }

  it should "ignore other keys, releases and other events" in {
    FolderViewApp.toAction(key(new KeyCode.Char('x'))) shouldBe None
    FolderViewApp.toAction(key(new KeyCode.Up(), KeyEventKind.Release)) shouldBe None
    FolderViewApp.toAction(new Event.Resize(80, 24)) shouldBe None
  }
