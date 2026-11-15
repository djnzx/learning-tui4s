package jnz

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SttySpec extends AnyFlatSpec with Matchers:

  "parseSize" should "read `rows cols` of `stty size` as (columns, rows)" in {
    Stty.parseSize("30 100\n") shouldBe Some((100, 30))
  }

  it should "reject output that is not two positive numbers" in {
    Stty.parseSize("") shouldBe None
    Stty.parseSize("stty: stdin isn't a terminal") shouldBe None
    Stty.parseSize("0 0") shouldBe None
    Stty.parseSize("30 100 7") shouldBe None
  }
