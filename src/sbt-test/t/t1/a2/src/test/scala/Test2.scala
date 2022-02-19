package a2

import org.scalatest.wordspec.AnyWordSpec

class Test2 extends AnyWordSpec {
  "test2" should {
    "y" in {
      sys.error("aaa")
      true
    }
  }
}
