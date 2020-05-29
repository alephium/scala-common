package org.alephium.util

import java.math.BigInteger

class U256(val value: BigInteger) extends AnyVal with Ordered[U256] {
  import U256.validate

  @inline def isZero: Boolean = value.signum() == 0

  def addUnsafe(that: U256): U256 = {
    val underlying = this.value.add(that.value)
    assume(validate(underlying))
    U256.unsafe(underlying)
  }

  def add(that: U256): Option[U256] = {
    val underlying = this.value.add(that.value)
    if (validate(underlying)) Some(U256.unsafe(underlying)) else None
  }

  def subUnsafe(that: U256): U256 = {
    val underlying = this.value.subtract(that.value)
    assume(validate(underlying))
    U256.unsafe(underlying)
  }

  def sub(that: U256): Option[U256] = {
    val underlying = this.value.subtract(that.value)
    if (validate(underlying)) Some(U256.unsafe(underlying)) else None
  }

  def mulUnsafe(that: U256): U256 = {
    val underlying = this.value.multiply(that.value)
    assume(validate(underlying))
    U256.unsafe(underlying)
  }

  def mul(that: U256): Option[U256] = {
    val underlying = this.value.multiply(that.value)
    if (validate(underlying)) Some(U256.unsafe(underlying)) else None
  }

  def divUnsafe(that: U256): U256 = {
    assume(!that.isZero)
    U256.unsafe(this.value.divide(that.value))
  }

  def div(that: U256): Option[U256] = {
    if (that.isZero) None
    else {
      Some(U256.unsafe(this.value.divide(that.value)))
    }
  }

  def modUnsafe(that: U256): U256 = {
    assume(!that.isZero)
    U256.unsafe(this.value.remainder(that.value))
  }

  def mod(that: U256): Option[U256] = {
    if (that.isZero) None else Some(U256.unsafe(this.value.remainder(that.value)))
  }

  def compare(that: U256): Int = this.value.compareTo(that.value)

  def toBigInt: BigInteger = value
}

object U256 {
  private[util] val upperBound = BigInteger.valueOf(2).pow(256)

  private[util] def validate(value: BigInteger): Boolean = {
    (value.signum() >= 0) && (value.compareTo(upperBound) < 0)
  }

  def unsafe(value: BigInteger): U256 = {
    assume(validate(value))
    new U256(value)
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
