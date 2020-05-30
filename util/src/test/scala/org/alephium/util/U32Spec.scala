package org.alephium.util

import java.math.BigInteger

class U32Spec extends AlephiumSpec {
  val numGen = (0 to 4).flatMap(i => List(i - 2, Int.MinValue + i, Int.MaxValue - i))

  it should "convert to BigInt" in {
    U32.Zero.toBigInt is BigInteger.ZERO
    U32.One.toBigInt is BigInteger.ONE
    U32.Two.toBigInt is BigInteger.TWO
    U32.MaxValue.toBigInt is BigInteger.TWO.pow(32).subtract(BigInteger.ONE)
  }

  def test(op: (U32, U32)                       => Option[U32],
           opUnsafe: (U32, U32)                 => U32,
           opExpected: (BigInteger, BigInteger) => BigInteger,
           condition: Int                       => Boolean = _ >= Int.MinValue): Unit = {
    for {
      a <- numGen
      b <- numGen
    } {
      val aU32          = U32.unsafe(a)
      val bU32          = U32.unsafe(b)
      lazy val expected = opExpected(aU32.toBigInt, bU32.toBigInt)
      if (condition(b) && expected >= BigInteger.ZERO && expected <= U32.MaxValue.toBigInt) {
        op(aU32, bU32).get.toBigInt is expected
        opUnsafe(aU32, bU32).toBigInt is expected
      } else {
        assertThrows[AssertionError](opUnsafe(aU32, bU32))
        op(aU32, bU32).isEmpty is true
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

  it should "compare U32" in {
    for {
      a <- numGen
      b <- numGen
    } {
      val aU32 = U32.unsafe(a)
      val bU32 = U32.unsafe(b)
      aU32.compareTo(bU32) is Integer.compareUnsigned(a, b)
    }
  }
}
