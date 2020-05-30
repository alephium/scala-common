package org.alephium.util

import java.math.BigInteger

class U256Spec extends AlephiumSpec {
  val numGen = (0 to 3).flatMap { i =>
    val n = BigInteger.valueOf(i.toLong)
    List(n, U256.MaxValue.divUnsafe(U256.Two).toBigInt.add(n), U256.MaxValue.toBigInt.subtract(n))
  }

  it should "be bounded properly" in {
    U256.upperBound.subtract(BigInteger.ZERO) is BigInteger.TWO.pow(256)
    U256.validate(BigInteger.ZERO) is true
    U256.validate(BigInteger.ONE) is true
    U256.validate(BigInteger.ONE.negate()) is false
    U256.validate(U256.upperBound) is false
    U256.validate(U256.upperBound.subtract(BigInteger.ONE)) is true
  }

  it should "convert to BigInt" in {
    U256.Zero.toBigInt is BigInteger.ZERO
    U256.One.toBigInt is BigInteger.ONE
    U256.MaxValue.toBigInt is BigInteger.TWO.pow(256).subtract(BigInteger.ONE)
  }

  def test(op: (U256, U256)                     => Option[U256],
           opUnsafe: (U256, U256)               => U256,
           opExpected: (BigInteger, BigInteger) => BigInteger,
           condition: BigInteger                => Boolean = _ >= BigInteger.ZERO): Unit = {
    for {
      a <- numGen
      b <- numGen
    } {
      val aU256         = U256.unsafe(a)
      val bU256         = U256.unsafe(b)
      lazy val expected = opExpected(aU256.toBigInt, bU256.toBigInt)
      if (condition(b) && expected >= BigInteger.ZERO && expected < U256.upperBound) {
        op(aU256, bU256).get.toBigInt is expected
        opUnsafe(aU256, bU256).toBigInt is expected
      } else {
        assertThrows[AssertionError](opUnsafe(aU256, bU256))
        op(aU256, bU256).isEmpty is true
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
    test(_.div(_), _.divUnsafe(_), _.divide(_), _ > BigInteger.ZERO)
  }

  it should "test mod" in {
    test(_.mod(_), _.modUnsafe(_), _.remainder(_), _ > BigInteger.ZERO)
  }

  it should "compare U256" in {
    for {
      a <- numGen
      b <- numGen
    } {
      val aU256 = U256.unsafe(a)
      val bU256 = U256.unsafe(b)
      aU256.compareTo(bU256) is a.compareTo(b)
    }
  }
}