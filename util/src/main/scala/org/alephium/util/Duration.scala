package org.alephium.util

import java.time.{Duration => JDuration}

import scala.concurrent.duration.{FiniteDuration => SDuration, MILLISECONDS}

class Duration(val millis: Long) extends AnyVal with Ordered[Duration] {
  def toSeconds: Long = {
    val seconds   = millis / 1000
    val remainder = millis % 1000
    if (remainder < 0) seconds - 1 else seconds
  }

  def toMinutes: Long = toSeconds / 60

  def toHours: Long = toSeconds / (60 * 60)

  // Note: if this is not in the bound of scala Duration, the method will throw an exception
  // Scala Duration is limited to +-(2^63-1)ns (ca. 292 years)
  def asScala: SDuration = SDuration.apply(millis, MILLISECONDS)

  def +(another: Duration): Duration = Duration(millis + another.millis)

  def -(another: Duration): Duration = Duration(millis - another.millis)

  def *(scale: Long): Duration = Duration(millis * scale)

  def /(scale: Long): Duration = Duration(millis / scale)

  def compare(that: Duration): Int = millis compare that.millis

  override def toString: String = s"${millis}ms"
}

object Duration {
  def apply(millis: Long): Duration = new Duration(millis)

  val zero: Duration = apply(0)

  def ofMillis(millis: Long): Duration = apply(millis)

  def ofSeconds(seconds: Long): Duration = apply(seconds * 1000)

  def ofMinutes(minutes: Long): Duration = apply(minutes * 60 * 1000)

  def ofHours(hours: Long): Duration = apply(hours * 60 * 60 * 1000)

  def from(dt: JDuration): Duration = ofMillis(dt.toMillis)
}
