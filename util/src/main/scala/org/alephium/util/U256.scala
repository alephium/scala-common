package org.alephium.util

import java.math.BigInteger

import akka.util.ByteString

class U256(val v: BigInteger) extends AnyVal with Ordered[U256] {
  import U256.validate

  @inline def isZero: Boolean = v.signum() == 0

  def addUnsafe(that: U256): U256 = {
    val underlying = this.v.add(that.v)
    assume(validate(underlying))
    U256.unsafe(underlying)
  }

  def add(that: U256): Option[U256] = {
    val underlying = this.v.add(that.v)
    if (validate(underlying)) Some(U256.unsafe(underlying)) else None
  }

  def subUnsafe(that: U256): U256 = {
    val underlying = this.v.subtract(that.v)
    assume(validate(underlying))
    U256.unsafe(underlying)
  }

  def sub(that: U256): Option[U256] = {
    val underlying = this.v.subtract(that.v)
    if (validate(underlying)) Some(U256.unsafe(underlying)) else None
  }

  def mulUnsafe(that: U256): U256 = {
    val underlying = this.v.multiply(that.v)
    assume(validate(underlying))
    U256.unsafe(underlying)
  }

  def mul(that: U256): Option[U256] = {
    val underlying = this.v.multiply(that.v)
    if (validate(underlying)) Some(U256.unsafe(underlying)) else None
  }

  def divUnsafe(that: U256): U256 = {
    assume(!that.isZero)
    U256.unsafe(this.v.divide(that.v))
  }

  def div(that: U256): Option[U256] = {
    if (that.isZero) None
    else {
      Some(U256.unsafe(this.v.divide(that.v)))
    }
  }

  def modUnsafe(that: U256): U256 = {
    assume(!that.isZero)
    U256.unsafe(this.v.remainder(that.v))
  }

  def mod(that: U256): Option[U256] = {
    if (that.isZero) None else Some(U256.unsafe(this.v.remainder(that.v)))
  }

  def compare(that: U256): Int = this.v.compareTo(that.v)

  def toBigInt: BigInteger = v

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def toBytes: ByteString = {
    val tmp           = ByteString.fromArrayUnsafe(v.toByteArray)
    val paddingLength = 32 - tmp.length
    if (paddingLength < 0) tmp.tail
    else if (paddingLength > 0) {
      ByteString.fromArrayUnsafe(Array.fill(paddingLength)(0)) ++ tmp
    } else tmp
  }
}

object U256 {
  private[util] val upperBound = BigInteger.ONE.shiftLeft(256)

  private[util] def validate(value: BigInteger): Boolean = {
    Numeric.nonNegative(value) && value.bitLength() <= 256
  }

  def unsafe(value: BigInteger): U256 = {
    assume(validate(value))
    new U256(value)
  }

  def unsafe(value: Long): U256 = {
    assume(value >= 0)
    new U256(BigInteger.valueOf(value))
  }

  def unsafe(bytes: ByteString): U256 = {
    assume(bytes.length == 32)
    new U256(new BigInteger(1, bytes.toArray))
  }

  def from(value: BigInteger): Option[U256] = {
    if (validate(value)) Some(new U256(value)) else None
  }

  val Zero: U256     = unsafe(BigInteger.ZERO)
  val One: U256      = unsafe(BigInteger.ONE)
  val Two: U256      = unsafe(BigInteger.TWO)
  val Ten: U256      = unsafe(BigInteger.TEN)
  val MaxValue: U256 = unsafe(upperBound.subtract(BigInteger.ONE))
  val MinValue: U256 = Zero
}
