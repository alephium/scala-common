package org.alephium.util

import java.math.BigInteger

class U64(val value: Long) extends AnyVal with Ordered[U64] {
  import java.lang.{Long => JLong}

  @inline def isZero: Boolean = value == 0

  def addUnsafe(that: U64): U64 = {
    val underlying = this.value + that.value
    assume(JLong.compareUnsigned(underlying, this.value) >= 0)
    U64.unsafe(underlying)
  }

  def add(that: U64): Option[U64] = {
    val underlying = this.value + that.value
    if (JLong.compareUnsigned(underlying, this.value) >= 0) Some(U64.unsafe(underlying)) else None
  }

  def subUnsafe(that: U64): U64 = {
    assume(JLong.compareUnsigned(this.value, that.value) >= 0)
    U64.unsafe(this.value - that.value)
  }

  def sub(that: U64): Option[U64] = {
    if (JLong.compareUnsigned(this.value, that.value) >= 0) {
      Some(U64.unsafe(this.value - that.value))
    } else None
  }

  def mulUnsafe(that: U64): U64 = {
    if (this.value == 0) U64.Zero
    else {
      val underlying = this.value * that.value
      assume(JLong.divideUnsigned(underlying, this.value) == that.value)
      U64.unsafe(underlying)
    }
  }

  def mul(that: U64): Option[U64] = {
    if (this.value == 0) Some(U64.Zero)
    else {
      val underlying = this.value * that.value
      if (JLong.divideUnsigned(underlying, this.value) == that.value) {
        Some(U64.unsafe(underlying))
      } else None
    }
  }

  def divUnsafe(that: U64): U64 = {
    assume(!that.isZero)
    U64.unsafe(JLong.divideUnsigned(this.value, that.value))
  }

  def div(that: U64): Option[U64] = {
    if (that.isZero) None else Some(U64.unsafe(JLong.divideUnsigned(this.value, that.value)))
  }

  def modUnsafe(that: U64): U64 = {
    assume(!that.isZero)
    U64.unsafe(JLong.remainderUnsigned(this.value, that.value))
  }

  def mod(that: U64): Option[U64] = {
    if (that.isZero) None else Some(U64.unsafe(JLong.remainderUnsigned(this.value, that.value)))
  }

  def compare(that: U64): Int = JLong.compareUnsigned(this.value, that.value)

  def toBigInt: BigInteger = {
    val bi = BigInteger.valueOf(value)
    if (value < 0) bi.add(U64.modulus) else bi
  }
}

object U64 {
  private[U64] val modulus = BigInteger.valueOf(1).shiftLeft(java.lang.Long.SIZE)

  def unsafe(value: Long): U64 = new U64(value)

  def from(value: Long): Option[U64] = if (value >= 0) Some(unsafe(value)) else None

  val Zero: U64     = unsafe(0)
  val One: U64      = unsafe(1)
  val MaxValue: U64 = unsafe(-1)
}
