package org.alephium.util

import java.math.BigInteger

class I64(val v: Long) extends AnyVal with Ordered[I64] {
  import java.lang.{Long => JLong}

  @inline def isZero: Boolean = v == 0

  def addUnsafe(that: I64): I64 = {
    val underlying = this.v + that.v
    assume(I64.checkAdd(this, that, underlying))
    I64.unsafe(underlying)
  }

  def add(that: I64): Option[I64] = {
    val underlying = this.v + that.v
    if (I64.checkAdd(this, that, underlying)) {
      Some(I64.unsafe(underlying))
    } else None
  }

  def subUnsafe(that: I64): I64 = {
    val underlying = this.v - that.v
    assume(I64.checkSub(this, that, underlying))
    I64.unsafe(underlying)
  }

  def sub(that: I64): Option[I64] = {
    val underlying = this.v - that.v
    if (I64.checkSub(this, that, underlying)) {
      Some(I64.unsafe(underlying))
    } else None
  }

  def mulUnsafe(that: I64): I64 = {
    if (this.v == 0) I64.Zero
    else {
      val underlying = this.v * that.v
      assume(I64.checkMul(this, that, underlying))
      I64.unsafe(underlying)
    }
  }

  def mul(that: I64): Option[I64] = {
    if (this.v == 0) Some(I64.Zero)
    else {
      val underlying = this.v * that.v
      if (I64.checkMul(this, that, underlying)) {
        Some(I64.unsafe(underlying))
      } else None
    }
  }

  def divUnsafe(that: I64): I64 = {
    assume(I64.checkDiv(this, that))
    I64.unsafe(this.v / that.v)
  }

  def div(that: I64): Option[I64] = {
    if (!I64.checkDiv(this, that)) None
    else Some(I64.unsafe(this.v / that.v))
  }

  def modUnsafe(that: I64): I64 = {
    assume(!that.isZero)
    I64.unsafe(this.v % that.v)
  }

  def mod(that: I64): Option[I64] = {
    if (that.isZero) None else Some(I64.unsafe(this.v % that.v))
  }

  def compare(that: I64): Int = JLong.compare(this.v, that.v)

  def toBigInt: BigInteger = BigInteger.valueOf(v)
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
    (b.v >= 0 && c >= a.v) || (b.v < 0 && c < a.v)
  }

  @inline private def checkSub(a: I64, b: I64, c: Long): Boolean = {
    (b.v >= 0 && c <= a.v) || (b.v < 0 && c > a.v)
  }

  // assume a != 0
  @inline private def checkMul(a: I64, b: I64, c: Long): Boolean = {
    !(a.v == -1 && b.v == Long.MinValue) && (c / a.v == b.v)
  }

  // assume b != 0
  @inline private def checkDiv(a: I64, b: I64): Boolean = {
    b.v != 0 && (b.v != -1 || a.v != Long.MinValue)
  }
}
