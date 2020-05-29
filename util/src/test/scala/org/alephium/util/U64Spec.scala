package org.alephium.util

import java.math.BigInteger

class U64Spec extends AlephiumSpec {
  val numGen = (0L to 3L).flatMap(i => List(i - 1, Long.MinValue + i, Long.MaxValue - i))

  it should "convert to BigInt" in {
    U64.Zero.toBigInt is BigInteger.ZERO
    U64.One.toBigInt is BigInteger.ONE
    U64.MaxValue.toBigInt is BigInteger.TWO.pow(64).subtract(BigInteger.ONE)
  }

  def test(op: (U64, U64)                       => Option[U64],
           opUnsafe: (U64, U64)                 => U64,
           opExpected: (BigInteger, BigInteger) => BigInteger,
           condition: Long                      => Boolean = _ >= Long.MinValue): Unit = {
    for {
      a <- numGen
      b <- numGen
    } {
      val aU64          = U64.unsafe(a)
      val bU64          = U64.unsafe(b)
      lazy val expected = opExpected(aU64.toBigInt, bU64.toBigInt)
      if (condition(b) && expected >= BigInteger.ZERO && expected <= U64.MaxValue.toBigInt) {
        op(aU64, bU64).get.toBigInt is expected
        opUnsafe(aU64, bU64).toBigInt is expected
      } else {
        assertThrows[AssertionError](opUnsafe(aU64, bU64))
        op(aU64, bU64).isEmpty is true
      }
    }
  }

  it should "test add" in {
    test(_.add(_), _.addUnsafe(_), _.add(_))
  }

  it should "test sub" in {
    test(_.sub(_), _.subUnsafe(_), _.subtract(_))
  }

  it should "test mul" in {
    test(_.mul(_), _.mulUnsafe(_), _.multiply(_))
  }

  it should "test div" in {
    test(_.div(_), _.divUnsafe(_), _.divide(_), _ != 0)
  }

  it should "test mod" in {
    test(_.mod(_), _.modUnsafe(_), _.remainder(_), _ != 0)
  }

  it should "compare U64" in {
    for {
      a <- numGen
      b <- numGen
    } {
      val aU64 = U64.unsafe(a)
      val bU64 = U64.unsafe(b)
      aU64.compareTo(bU64) is java.lang.Long.compareUnsigned(a, b)
    }
  }
}
