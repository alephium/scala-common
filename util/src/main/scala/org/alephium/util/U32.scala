package org.alephium.util

import java.math.BigInteger

class U32(val value: Int) extends AnyVal with Ordered[U32] {
  import java.lang.{Integer => JInt}

  @inline def isZero: Boolean = value == 0

  def addUnsafe(that: U32): U32 = {
    val underlying = this.value + that.value
    assume(JInt.compareUnsigned(underlying, this.value) >= 0)
    U32.unsafe(underlying)
  }

  def add(that: U32): Option[U32] = {
    val underlying = this.value + that.value
    if (JInt.compareUnsigned(underlying, this.value) >= 0) Some(U32.unsafe(underlying)) else None
  }

  def subUnsafe(that: U32): U32 = {
    assume(JInt.compareUnsigned(this.value, that.value) >= 0)
    U32.unsafe(this.value - that.value)
  }

  def sub(that: U32): Option[U32] = {
    if (JInt.compareUnsigned(this.value, that.value) >= 0) {
      Some(U32.unsafe(this.value - that.value))
    } else None
  }

  def mulUnsafe(that: U32): U32 = {
    if (this.value == 0) U32.Zero
    else {
      val underlying = this.value * that.value
      assume(JInt.divideUnsigned(underlying, this.value) == that.value)
      U32.unsafe(underlying)
    }
  }

  def mul(that: U32): Option[U32] = {
    if (this.value == 0) Some(U32.Zero)
    else {
      val underlying = this.value * that.value
      if (JInt.divideUnsigned(underlying, this.value) == that.value) {
        Some(U32.unsafe(underlying))
      } else None
    }
  }

  def divUnsafe(that: U32): U32 = {
    assume(!that.isZero)
    U32.unsafe(JInt.divideUnsigned(this.value, that.value))
  }

  def div(that: U32): Option[U32] = {
    if (that.isZero) None else Some(U32.unsafe(JInt.divideUnsigned(this.value, that.value)))
  }

  def modUnsafe(that: U32): U32 = {
    assume(!that.isZero)
    U32.unsafe(JInt.remainderUnsigned(this.value, that.value))
  }

  def mod(that: U32): Option[U32] = {
    if (that.isZero) None else Some(U32.unsafe(JInt.remainderUnsigned(this.value, that.value)))
  }

  def compare(that: U32): Int = JInt.compareUnsigned(this.value, that.value)

  def toBigInt: BigInteger = {
    BigInteger.valueOf(Integer.toUnsignedLong(value))
  }
}

object U32 {
  def unsafe(value: Int): U32 = new U32(value)

  def from(value: Int): Option[U32] = if (value >= 0) Some(unsafe(value)) else None

  val Zero: U32     = unsafe(0)
  val One: U32      = unsafe(1)
  val MaxValue: U32 = unsafe(-1)
}
