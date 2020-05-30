package org.alephium.util

import java.math.BigInteger

class U32(val value: Int) extends AnyVal with Ordered[U32] {
  import java.lang.{Integer => JInt}

  @inline def isZero: Boolean = value == 0

  def addUnsafe(that: U32): U32 = {
    val underlying = this.value + that.value
    assume(U32.checkAdd(this, underlying))
    U32.unsafe(underlying)
  }

  def add(that: U32): Option[U32] = {
    val underlying = this.value + that.value
    if (U32.checkAdd(this, underlying)) Some(U32.unsafe(underlying)) else None
  }

  def subUnsafe(that: U32): U32 = {
    assume(U32.checkSub(this, that))
    U32.unsafe(this.value - that.value)
  }

  def sub(that: U32): Option[U32] = {
    if (U32.checkSub(this, that)) {
      Some(U32.unsafe(this.value - that.value))
    } else None
  }

  def mulUnsafe(that: U32): U32 = {
    if (this.value == 0) U32.Zero
    else {
      val underlying = this.value * that.value
      assume(U32.checkMul(this, that, underlying))
      U32.unsafe(underlying)
    }
  }

  def mul(that: U32): Option[U32] = {
    if (this.value == 0) Some(U32.Zero)
    else {
      val underlying = this.value * that.value
      if (U32.checkMul(this, that, underlying)) {
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
  import java.lang.{Integer => JInt}

  def unsafe(value: Int): U32 = new U32(value)

  def from(value: Int): Option[U32] = if (value >= 0) Some(unsafe(value)) else None

  val Zero: U32     = unsafe(0)
  val One: U32      = unsafe(1)
  val Two: U32      = unsafe(2)
  val MaxValue: U32 = unsafe(-1)

  @inline private def checkAdd(a: U32, c: Int): Boolean = {
    JInt.compareUnsigned(c, a.value) >= 0
  }

  @inline private def checkSub(a: U32, b: U32): Boolean = {
    JInt.compareUnsigned(a.value, b.value) >= 0
  }

  // assume a != 0
  @inline private def checkMul(a: U32, b: U32, c: Int): Boolean = {
    JInt.divideUnsigned(c, a.value) == b.value
  }
}
