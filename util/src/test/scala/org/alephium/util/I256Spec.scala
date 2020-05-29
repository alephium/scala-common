package org.alephium.util

import java.math.BigInteger

class I256Spec extends AlephiumSpec {
  val numGen = (0L to 3L).flatMap { i =>
    val n = BigInteger.valueOf(i)
    List(n.subtract(BigInteger.ONE),
         I256.MinValue.toBigInt.add(n),
         I256.MaxValue.toBigInt.subtract(n))
  }

  it should "be bounded properly" in {
    I256.upperBound.subtract(I256.lowerBound) is BigInteger.TWO.pow(256)
    I256.validate(I256.lowerBound) is true
    I256.validate(I256.lowerBound.add(BigInteger.ONE)) is true
    I256.validate(I256.lowerBound.subtract(BigInteger.ONE)) is false
    I256.validate(I256.upperBound) is false
    I256.validate(I256.upperBound.subtract(BigInteger.ONE)) is true
  }

  it should "convert to BigInt" in {
    I256.Zero.toBigInt is BigInteger.ZERO
    I256.One.toBigInt is BigInteger.ONE
    I256.MaxValue.toBigInt is BigInteger.TWO.pow(255).subtract(BigInteger.ONE)
    I256.MinValue.toBigInt is BigInteger.TWO.pow(255).negate()
  }

  def test(
      op: (I256, I256)                     => Option[I256],
      opUnsafe: (I256, I256)               => I256,
      opExpected: (BigInteger, BigInteger) => BigInteger,
      condition: (BigInteger, BigInteger)  => Boolean = _ >= I256.lowerBound && _ >= I256.lowerBound)
    : Unit = {
    for {
      a <- numGen
      b <- numGen
    } {
      val aI256         = I256.unsafe(a)
      val bI256         = I256.unsafe(b)
      lazy val expected = opExpected(aI256.toBigInt, bI256.toBigInt)
      if (condition(a, b) && expected >= I256.lowerBound && expected < I256.upperBound) {
        op(aI256, bI256).get.toBigInt is expected
        opUnsafe(aI256, bI256).toBigInt is expected
      } else {
        assertThrows[AssertionError](opUnsafe(aI256, bI256))
        op(aI256, bI256).isEmpty is true
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
    test(_.div(_),
         _.divUnsafe(_),
         _.divide(_),
         (a, b) =>
           b != BigInteger.ZERO && !(a.equals(I256.lowerBound) && b.equals(I256.NegOne.toBigInt)))
  }

  it should "test mod" in {
    test(_.mod(_),
         _.modUnsafe(_),
         _.remainder(_),
         (a, b) =>
           b != BigInteger.ZERO && !(a.equals(I256.lowerBound) && b.equals(I256.NegOne.toBigInt)))
  }

  it should "compare I256" in {
    for {
      a <- numGen
      b <- numGen
    } {
      val aI256 = I256.unsafe(a)
      val bI256 = I256.unsafe(b)
      aI256.compareTo(bI256) is a.compareTo(b)
    }
  }
}
