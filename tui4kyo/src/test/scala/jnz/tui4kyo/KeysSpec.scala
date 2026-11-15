package jnz.tui4kyo

import jnz.Action
import kyo.UI.Keyboard
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KeysSpec extends AnyFlatSpec with Matchers:

  "toAction" should "map the keys of the spec" in {
    FolderViewApp.toAction(Keyboard.ArrowUp) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(Keyboard.Char('k')) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(Keyboard.ArrowDown) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(Keyboard.Char('j')) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(Keyboard.PageUp) shouldBe Some(Action.PageUp)
    FolderViewApp.toAction(Keyboard.PageDown) shouldBe Some(Action.PageDown)
    FolderViewApp.toAction(Keyboard.Home) shouldBe Some(Action.Home)
    FolderViewApp.toAction(Keyboard.End) shouldBe Some(Action.End)
    FolderViewApp.toAction(Keyboard.Enter) shouldBe Some(Action.Open)
    FolderViewApp.toAction(Keyboard.ArrowRight) shouldBe Some(Action.Open)
    FolderViewApp.toAction(Keyboard.Backspace) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(Keyboard.ArrowLeft) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(Keyboard.F10) shouldBe Some(Action.Quit)
    FolderViewApp.toAction(Keyboard.Char('q')) shouldBe Some(Action.Quit)
  }

  it should "ignore other keys" in {
    FolderViewApp.toAction(Keyboard.Char('x')) shouldBe None
    FolderViewApp.toAction(Keyboard.Tab) shouldBe None
    FolderViewApp.toAction(Keyboard.Escape) shouldBe None
  }
