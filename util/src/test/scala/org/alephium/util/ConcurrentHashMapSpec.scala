package org.alephium.util

class ConcurrentHashMapSpec extends AlephiumSpec {
  trait Fixture {
    val map = ConcurrentHashMap.empty[Int, Long]
  }

  it should "put / remove /contains" in new Fixture {
    forAll { (k: Int, v: Long) =>
      map.contains(k) is false
      map.add(k, v)
      map.contains(k) is true
      assertThrows[AssertionError](map.add(k, v))
      map.remove(k)
      map.contains(k) is false
      assertThrows[AssertionError](map.remove(k))
      map.removeIfExist(k)
    }
  }
}
