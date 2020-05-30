package org.alephium.util

import java.math.BigInteger

class I32(val value: Int) extends AnyVal with Ordered[I32] {
  import java.lang.{Integer => JInt}

  @inline def isZero: Boolean = value == 0

  def addUnsafe(that: I32): I32 = {
    val underlying = this.value + that.value
    assume(I32.checkAdd(this, that, underlying))
    I32.unsafe(underlying)
  }

  def add(that: I32): Option[I32] = {
    val underlying = this.value + that.value
    if (I32.checkAdd(this, that, underlying)) {
      Some(I32.unsafe(underlying))
    } else None
  }

  def subUnsafe(that: I32): I32 = {
    val underlying = this.value - that.value
    assume(I32.checkSub(this, that, underlying))
    I32.unsafe(underlying)
  }

  def sub(that: I32): Option[I32] = {
    val underlying = this.value - that.value
    if (I32.checkSub(this, that, underlying)) {
      Some(I32.unsafe(underlying))
    } else None
  }

  def mulUnsafe(that: I32): I32 = {
    if (this.value == 0) I32.Zero
    else {
      val underlying = this.value * that.value
      assume(I32.checkMul(this, that, underlying))
      I32.unsafe(underlying)
    }
  }

  def mul(that: I32): Option[I32] = {
    if (this.value == 0) Some(I32.Zero)
    else {
      val underlying = this.value * that.value
      if (I32.checkMul(this, that, underlying)) {
        Some(I32.unsafe(underlying))
      } else None
    }
  }

  def divUnsafe(that: I32): I32 = {
    assume(I32.checkDiv(this, that))
    I32.unsafe(this.value / that.value)
  }

  def div(that: I32): Option[I32] = {
    if (!I32.checkDiv(this, that)) None
    else Some(I32.unsafe(this.value / that.value))
  }

  def modUnsafe(that: I32): I32 = {
    assume(!that.isZero)
    I32.unsafe(this.value % that.value)
  }

  def mod(that: I32): Option[I32] = {
    if (that.isZero) None else Some(I32.unsafe(this.value % that.value))
  }

  def compare(that: I32): Int = JInt.compare(this.value, that.value)

  def toBigInt: BigInteger = BigInteger.valueOf(value.toLong)
}

object I32 {
  def unsafe(value: Int): I32 = new I32(value)

  def from(value: Int): Option[I32] = if (value >= 0) Some(unsafe(value)) else None

  val Zero: I32     = unsafe(0)
  val One: I32      = unsafe(1)
  val Two: I32      = unsafe(2)
  val NegOne: I32   = unsafe(-1)
  val MinValue: I32 = unsafe(Int.MinValue)
  val MaxValue: I32 = unsafe(Int.MaxValue)

  @inline private def checkAdd(a: I32, b: I32, c: Int): Boolean = {
    (b.value >= 0 && c >= a.value) || (b.value < 0 && c < a.value)
  }

  @inline private def checkSub(a: I32, b: I32, c: Int): Boolean = {
    (b.value >= 0 && c <= a.value) || (b.value < 0 && c > a.value)
  }

  // assume a != 0
  @inline private def checkMul(a: I32, b: I32, c: Int): Boolean = {
    !(a.value == -1 && b.value == Int.MinValue) && (c / a.value == b.value)
  }

  // assume b != 0
  @inline private def checkDiv(a: I32, b: I32): Boolean = {
    b.value != 0 && (b.value != -1 || a.value != Int.MinValue)
  }
}
