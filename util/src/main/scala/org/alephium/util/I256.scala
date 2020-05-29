package org.alephium.util

import java.math.BigInteger

class I256(val value: BigInteger) extends AnyVal with Ordered[I256] {
  import I256.validate

  @inline def isZero: Boolean = value.signum() == 0

  def addUnsafe(that: I256): I256 = {
    val underlying = this.value.add(that.value)
    assume(validate(underlying))
    I256.unsafe(underlying)
  }

  def add(that: I256): Option[I256] = {
    val underlying = this.value.add(that.value)
    if (validate(underlying)) Some(I256.unsafe(underlying)) else None
  }

  def subUnsafe(that: I256): I256 = {
    val underlying = this.value.subtract(that.value)
    assume(validate(underlying))
    I256.unsafe(underlying)
  }

  def sub(that: I256): Option[I256] = {
    val underlying = this.value.subtract(that.value)
    if (validate(underlying)) Some(I256.unsafe(underlying)) else None
  }

  def mulUnsafe(that: I256): I256 = {
    val underlying = this.value.multiply(that.value)
    assume(validate(underlying))
    I256.unsafe(underlying)
  }

  def mul(that: I256): Option[I256] = {
    val underlying = this.value.multiply(that.value)
    if (validate(underlying)) Some(I256.unsafe(underlying)) else None
  }

  def divUnsafe(that: I256): I256 = {
    assume(!(that.isZero || (that == I256.NegOne && this == I256.MinValue)))
    I256.unsafe(this.value.divide(that.value))
  }

  def div(that: I256): Option[I256] = {
    if (that.isZero || (that == I256.NegOne && this == I256.MinValue)) {
      None
    } else {
      Some(I256.unsafe(this.value.divide(that.value)))
    }
  }

  def modUnsafe(that: I256): I256 = {
    assume(!(that.isZero || (this.value == I256.lowerBound && that.value == I256.NegOne.toBigInt)))
    I256.unsafe(this.value.remainder(that.value))
  }

  def mod(that: I256): Option[I256] = {
    if (that.isZero || (this.value == I256.lowerBound && that.value == I256.NegOne.toBigInt)) None
    else Some(I256.unsafe(this.value.remainder(that.value)))
  }

  def compare(that: I256): Int = this.value.compareTo(that.value)

  def toBigInt: BigInteger = value
}

object I256 {
  // scalastyle:off magic.number
  private[util] val upperBound = BigInteger.valueOf(2).pow(255) // exclusive
  private[util] val lowerBound = upperBound.negate() // inclusive
  // scalastyle:on magic.number

  private[util] def validate(value: BigInteger): Boolean = {
    (value.compareTo(lowerBound) >= 0) && (value.compareTo(upperBound) < 0)
  }

  def unsafe(value: BigInteger): I256 = {
    assume(validate(value))
    new I256(value)
  }

  def from(value: BigInteger): Option[I256] = {
    if (validate(value)) Some(new I256(value)) else None
  }

  val Zero: I256     = unsafe(BigInteger.ZERO)
  val One: I256      = unsafe(BigInteger.ONE)
  val Two: I256      = unsafe(BigInteger.TWO)
  val NegOne: I256   = unsafe(BigInteger.ONE.negate())
  val MaxValue: I256 = unsafe(upperBound.subtract(BigInteger.ONE))
  val MinValue: I256 = unsafe(lowerBound)
}
