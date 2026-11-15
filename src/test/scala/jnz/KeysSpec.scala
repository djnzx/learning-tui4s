package jnz

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KeysSpec extends AnyFlatSpec with Matchers:

  private def key(t: KeyType): KeyStroke = KeyStroke(t)
  private def char(c: Char): KeyStroke   = KeyStroke(c, false, false)

  "toAction" should "map the keys of the spec" in {
    FolderViewApp.toAction(key(KeyType.ArrowUp)) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(char('k')) shouldBe Some(Action.LineUp)
    FolderViewApp.toAction(key(KeyType.ArrowDown)) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(char('j')) shouldBe Some(Action.LineDown)
    FolderViewApp.toAction(key(KeyType.PageUp)) shouldBe Some(Action.PageUp)
    FolderViewApp.toAction(key(KeyType.PageDown)) shouldBe Some(Action.PageDown)
    FolderViewApp.toAction(key(KeyType.Home)) shouldBe Some(Action.Home)
    FolderViewApp.toAction(key(KeyType.End)) shouldBe Some(Action.End)
    FolderViewApp.toAction(key(KeyType.Enter)) shouldBe Some(Action.Open)
    FolderViewApp.toAction(key(KeyType.ArrowRight)) shouldBe Some(Action.Open)
    FolderViewApp.toAction(key(KeyType.Backspace)) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(key(KeyType.ArrowLeft)) shouldBe Some(Action.Parent)
    FolderViewApp.toAction(key(KeyType.F10)) shouldBe Some(Action.Quit)
    FolderViewApp.toAction(char('q')) shouldBe Some(Action.Quit)
    FolderViewApp.toAction(KeyStroke('c', true, false)) shouldBe Some(Action.Quit)
    FolderViewApp.toAction(key(KeyType.EOF)) shouldBe Some(Action.Quit)
  }

  it should "ignore other keys" in {
    FolderViewApp.toAction(char('x')) shouldBe None
    FolderViewApp.toAction(key(KeyType.Tab)) shouldBe None
    FolderViewApp.toAction(key(KeyType.Escape)) shouldBe None
  }
