package org.alephium.util

import java.math.BigInteger

import akka.util.ByteString

class I256(val v: BigInteger) extends AnyVal with Ordered[I256] {
  import I256.validate

  @inline def isZero: Boolean = v.signum() == 0

  def addUnsafe(that: I256): I256 = {
    val underlying = this.v.add(that.v)
    assume(validate(underlying))
    I256.unsafe(underlying)
  }

  def add(that: I256): Option[I256] = {
    val underlying = this.v.add(that.v)
    if (validate(underlying)) Some(I256.unsafe(underlying)) else None
  }

  def subUnsafe(that: I256): I256 = {
    val underlying = this.v.subtract(that.v)
    assume(validate(underlying))
    I256.unsafe(underlying)
  }

  def sub(that: I256): Option[I256] = {
    val underlying = this.v.subtract(that.v)
    if (validate(underlying)) Some(I256.unsafe(underlying)) else None
  }

  def mulUnsafe(that: I256): I256 = {
    val underlying = this.v.multiply(that.v)
    assume(validate(underlying))
    I256.unsafe(underlying)
  }

  def mul(that: I256): Option[I256] = {
    val underlying = this.v.multiply(that.v)
    if (validate(underlying)) Some(I256.unsafe(underlying)) else None
  }

  def divUnsafe(that: I256): I256 = {
    assume(!(that.isZero || (that == I256.NegOne && this == I256.MinValue)))
    I256.unsafe(this.v.divide(that.v))
  }

  def div(that: I256): Option[I256] = {
    if (that.isZero || (that == I256.NegOne && this == I256.MinValue)) {
      None
    } else {
      Some(I256.unsafe(this.v.divide(that.v)))
    }
  }

  def modUnsafe(that: I256): I256 = {
    assume(!(that.isZero || (this.v == I256.lowerBound && that.v == I256.NegOne.toBigInt)))
    I256.unsafe(this.v.remainder(that.v))
  }

  def mod(that: I256): Option[I256] = {
    if (that.isZero || (this.v == I256.lowerBound && that.v == I256.NegOne.toBigInt)) None
    else Some(I256.unsafe(this.v.remainder(that.v)))
  }

  def compare(that: I256): Int = this.v.compareTo(that.v)

  def toBigInt: BigInteger = v

  def toBytes: ByteString = {
    val tmp           = ByteString.fromArrayUnsafe(v.toByteArray)
    val paddingLength = 32 - tmp.length
    if (paddingLength > 0) {
      if (this >= I256.Zero) {
        ByteString.fromArrayUnsafe(Array.fill(paddingLength)(0)) ++ tmp
      } else {
        ByteString.fromArrayUnsafe(Array.fill(paddingLength)(0xff.toByte)) ++ tmp
      }
    } else {
      assume(paddingLength == 0)
      tmp
    }
  }
}

object I256 {
  // scalastyle:off magic.number
  private[util] val upperBound = BigInteger.ONE.shiftLeft(255) // exclusive
  private[util] val lowerBound = upperBound.negate() // inclusive
  // scalastyle:on magic.number

  private[util] def validate(value: BigInteger): Boolean = {
    value.bitLength() <= 255
  }

  def unsafe(value: BigInteger): I256 = {
    assume(validate(value))
    new I256(value)
  }

  def unsafe(bytes: ByteString): I256 = {
    assume(bytes.length == 32)
    new I256(new BigInteger(bytes.toArray))
  }

  def from(value: BigInteger): Option[I256] = {
    if (validate(value)) Some(new I256(value)) else None
  }

  def from(value: Long): I256 = {
    new I256(BigInteger.valueOf(value))
  }

  val Zero: I256     = unsafe(BigInteger.ZERO)
  val One: I256      = unsafe(BigInteger.ONE)
  val Two: I256      = unsafe(BigInteger.TWO)
  val NegOne: I256   = unsafe(BigInteger.ONE.negate())
  val MaxValue: I256 = unsafe(upperBound.subtract(BigInteger.ONE))
  val MinValue: I256 = unsafe(lowerBound)
}
