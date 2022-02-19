package a1

import org.scalatest.wordspec.AnyWordSpec

class Test1 extends AnyWordSpec {
  "test1" should {
    "x1" in {
      assert(List("a").size == 2)
    }
    "x2" in {
      assert(true)
    }
    "x3" in {
      assert(List("b").isEmpty)
    }
  }
}
