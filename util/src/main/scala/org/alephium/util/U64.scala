package org.alephium.util

import java.math.BigInteger

class U64(val v: Long) extends AnyVal with Ordered[U64] {
  import java.lang.{Long => JLong}

  @inline def isZero: Boolean = v == 0

  def addUnsafe(that: U64): U64 = {
    val underlying = this.v + that.v
    assume(U64.checkAdd(this, underlying))
    U64.unsafe(underlying)
  }

  def add(that: U64): Option[U64] = {
    val underlying = this.v + that.v
    if (U64.checkAdd(this, underlying)) Some(U64.unsafe(underlying)) else None
  }

  def subUnsafe(that: U64): U64 = {
    assume(U64.checkSub(this, that))
    U64.unsafe(this.v - that.v)
  }

  def sub(that: U64): Option[U64] = {
    if (U64.checkSub(this, that)) {
      Some(U64.unsafe(this.v - that.v))
    } else None
  }

  def mulUnsafe(that: U64): U64 = {
    if (this.v == 0) U64.Zero
    else {
      val underlying = this.v * that.v
      assume(U64.checkMul(this, that, underlying))
      U64.unsafe(underlying)
    }
  }

  def mul(that: U64): Option[U64] = {
    if (this.v == 0) Some(U64.Zero)
    else {
      val underlying = this.v * that.v
      if (U64.checkMul(this, that, underlying)) {
        Some(U64.unsafe(underlying))
      } else None
    }
  }

  def divUnsafe(that: U64): U64 = {
    assume(!that.isZero)
    U64.unsafe(JLong.divideUnsigned(this.v, that.v))
  }

  def div(that: U64): Option[U64] = {
    if (that.isZero) None else Some(U64.unsafe(JLong.divideUnsigned(this.v, that.v)))
  }

  def modUnsafe(that: U64): U64 = {
    assume(!that.isZero)
    U64.unsafe(JLong.remainderUnsigned(this.v, that.v))
  }

  def mod(that: U64): Option[U64] = {
    if (that.isZero) None else Some(U64.unsafe(JLong.remainderUnsigned(this.v, that.v)))
  }

  def compare(that: U64): Int = JLong.compareUnsigned(this.v, that.v)

  def toBigInt: BigInteger = {
    val bi = BigInteger.valueOf(v)
    if (v < 0) bi.add(U64.modulus) else bi
  }
}

object U64 {
  import java.lang.{Long => JLong}

  private[U64] val modulus = BigInteger.valueOf(1).shiftLeft(java.lang.Long.SIZE)

  def unsafe(value: Long): U64 = new U64(value)

  def from(value: Long): Option[U64] = if (value >= 0) Some(unsafe(value)) else None

  val Zero: U64     = unsafe(0)
  val One: U64      = unsafe(1)
  val Two: U64      = unsafe(2)
  val MaxValue: U64 = unsafe(-1)

  @inline private def checkAdd(a: U64, c: Long): Boolean = {
    JLong.compareUnsigned(c, a.v) >= 0
  }

  @inline private def checkSub(a: U64, b: U64): Boolean = {
    JLong.compareUnsigned(a.v, b.v) >= 0
  }

  // assume a != 0
  @inline private def checkMul(a: U64, b: U64, c: Long): Boolean = {
    JLong.divideUnsigned(c, a.v) == b.v
  }
}
