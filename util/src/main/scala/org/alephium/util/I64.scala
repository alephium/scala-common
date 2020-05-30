package org.alephium.util

import java.math.BigInteger

class I64(val value: Long) extends AnyVal with Ordered[I64] {
  import java.lang.{Long => JLong}

  @inline def isZero: Boolean = value == 0

  def addUnsafe(that: I64): I64 = {
    val underlying = this.value + that.value
    assume(I64.checkAdd(this, that, underlying))
    I64.unsafe(underlying)
  }

  def add(that: I64): Option[I64] = {
    val underlying = this.value + that.value
    if (I64.checkAdd(this, that, underlying)) {
      Some(I64.unsafe(underlying))
    } else None
  }

  def subUnsafe(that: I64): I64 = {
    val underlying = this.value - that.value
    assume(I64.checkSub(this, that, underlying))
    I64.unsafe(underlying)
  }

  def sub(that: I64): Option[I64] = {
    val underlying = this.value - that.value
    if (I64.checkSub(this, that, underlying)) {
      Some(I64.unsafe(underlying))
    } else None
  }

  def mulUnsafe(that: I64): I64 = {
    if (this.value == 0) I64.Zero
    else {
      val underlying = this.value * that.value
      assume(I64.checkMul(this, that, underlying))
      I64.unsafe(underlying)
    }
  }

  def mul(that: I64): Option[I64] = {
    if (this.value == 0) Some(I64.Zero)
    else {
      val underlying = this.value * that.value
      if (I64.checkMul(this, that, underlying)) {
        Some(I64.unsafe(underlying))
      } else None
    }
  }

  def divUnsafe(that: I64): I64 = {
    assume(I64.checkDiv(this, that))
    I64.unsafe(this.value / that.value)
  }

  def div(that: I64): Option[I64] = {
    if (!I64.checkDiv(this, that)) None
    else Some(I64.unsafe(this.value / that.value))
  }

  def modUnsafe(that: I64): I64 = {
    assume(!that.isZero)
    I64.unsafe(this.value % that.value)
  }

  def mod(that: I64): Option[I64] = {
    if (that.isZero) None else Some(I64.unsafe(this.value % that.value))
  }

  def compare(that: I64): Int = JLong.compare(this.value, that.value)

  def toBigInt: BigInteger = BigInteger.valueOf(value)
}

object I64 {
  def unsafe(value: Long): I64 = new I64(value)

  def from(value: Long): Option[I64] = if (value >= 0) Some(unsafe(value)) else None

  val Zero: I64     = unsafe(0)
  val One: I64      = unsafe(1)
  val Two: I64      = unsafe(2)
  val NegOne: I64   = unsafe(-1)
  val MinValue: I64 = unsafe(Long.MinValue)
  val MaxValue: I64 = unsafe(Long.MaxValue)

  @inline private def checkAdd(a: I64, b: I64, c: Long): Boolean = {
    (b.value >= 0 && c >= a.value) || (b.value < 0 && c < a.value)
  }

  @inline private def checkSub(a: I64, b: I64, c: Long): Boolean = {
    (b.value >= 0 && c <= a.value) || (b.value < 0 && c > a.value)
  }

  // assume a != 0
  @inline private def checkMul(a: I64, b: I64, c: Long): Boolean = {
    !(a.value == -1 && b.value == Long.MinValue) && (c / a.value == b.value)
  }

  // assume b != 0
  @inline private def checkDiv(a: I64, b: I64): Boolean = {
    b.value != 0 && (b.value != -1 || a.value != Long.MinValue)
  }
}
