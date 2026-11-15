package jnz

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import termflow.tui.KeyDecoder.InputKey

class KeysSpec extends AnyFlatSpec with Matchers:

  "toAction" should "map the keys of the spec" in {
    FolderViewApp.toAction(InputKey.ArrowUp) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(InputKey.CharKey('k')) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(InputKey.ArrowDown) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(InputKey.CharKey('j')) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(InputKey.PageUp) shouldBe Some(Action.PageUp)
    FolderViewApp.toAction(InputKey.PageDown) shouldBe Some(Action.PageDown)
    FolderViewApp.toAction(InputKey.Home) shouldBe Some(Action.Home)
    FolderViewApp.toAction(InputKey.End) shouldBe Some(Action.End)
    FolderViewApp.toAction(InputKey.Enter) shouldBe Some(Action.Open)
    FolderViewApp.toAction(InputKey.ArrowRight) shouldBe Some(Action.Open)
    FolderViewApp.toAction(InputKey.Backspace) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(InputKey.ArrowLeft) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(InputKey.F10) shouldBe Some(Action.Quit)
    FolderViewApp.toAction(InputKey.CharKey('q')) shouldBe Some(Action.Quit)
  }

  it should "ignore other keys" in {
    FolderViewApp.toAction(InputKey.CharKey('x')) shouldBe None
    FolderViewApp.toAction(InputKey.Tab) shouldBe None
  }
