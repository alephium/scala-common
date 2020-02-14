package org.alephium.util

class TimeStamp(val millis: Long) extends AnyVal with Ordered[TimeStamp] {
  def plusMillis(millisToAdd: Long): Option[TimeStamp] =
    TimeStamp.from(millis + millisToAdd)

  def plusSeconds(secondsToAdd: Long): Option[TimeStamp] =
    TimeStamp.from(millis + secondsToAdd * 1000)

  def plusMinutes(minutesToAdd: Long): Option[TimeStamp] =
    TimeStamp.from(millis + minutesToAdd * 60 * 1000)

  def plusHours(hoursToAdd: Long): Option[TimeStamp] =
    TimeStamp.from(millis + hoursToAdd * 60 * 60 * 1000)

  def +(duration: Duration): Option[TimeStamp] =
    TimeStamp.from(millis + duration.millis)

  def -(duration: Duration): Option[TimeStamp] =
    TimeStamp.from(millis - duration.millis)

  def diff(another: TimeStamp): Duration =
    Duration.ofMillis(millis - another.millis)

  def isBefore(another: TimeStamp): Boolean =
    millis < another.millis

  def compare(that: TimeStamp): Int = millis compare that.millis
}

object TimeStamp {
  val zero: TimeStamp = unsafeFrom(0)

  def now(): TimeStamp = unsafeFrom(System.currentTimeMillis())

  def unsafeFrom(millis: Long): TimeStamp = {
    assume(millis >= 0, "timestamp should be non-negative")
    new TimeStamp(millis)
  }

  def from(millis: Long): Option[TimeStamp] = {
    if (millis < 0) None else Some(new TimeStamp(millis))
  }
}
