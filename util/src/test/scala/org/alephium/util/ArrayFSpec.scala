package org.alephium.util

class ArrayFSpec extends AlephiumSpec {
  it should "check array index" in {
    forAll { array: Array[Int] =>
      ArrayF.checkIndex(array, -1) is false
      ArrayF.checkIndex(array, array.length) is false
      if (array.nonEmpty) {
        ArrayF.checkIndex(array, 0) is true
        ArrayF.checkIndex(array, array.length - 1) is true
      }
    }
  }

  it should "get element safely" in {
    forAll { array: Array[Int] =>
      ArrayF.get(array, -1) is None
      ArrayF.get(array, array.length) is None
      if (array.nonEmpty) {
        ArrayF.get(array, 0) is Some(array(0))
        ArrayF.get(array, array.length - 1) is Some(array(array.length - 1))
      }
    }
  }
}
