package jnz

import com.googlecode.lanterna.TerminalSize
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SttyTerminalSpec extends AnyFlatSpec with Matchers:

  "parseSize" should "read `rows cols` of `stty size`" in {
    SttyTerminal.parseSize("30 100\n") shouldBe Some(TerminalSize(100, 30))
  }

  it should "reject output that is not two positive numbers" in {
    SttyTerminal.parseSize("") shouldBe None
    SttyTerminal.parseSize("stty: stdin isn't a terminal") shouldBe None
    SttyTerminal.parseSize("0 0") shouldBe None
    SttyTerminal.parseSize("30 100 7") shouldBe None
  }
