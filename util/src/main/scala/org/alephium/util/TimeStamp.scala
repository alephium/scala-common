package org.alephium.util

class TimeStamp(val millis: Long) extends AnyVal with Ordered[TimeStamp] {
  def plusMillis(millisToAdd: Long): Option[TimeStamp] =
    TimeStamp.ofMillis(millis + millisToAdd)

  def plusMillisUnsafe(millisToAdd: Long): TimeStamp = {
    TimeStamp.ofMillisUnsafe(millis + millisToAdd)
  }

  def plusSeconds(secondsToAdd: Long): Option[TimeStamp] =
    TimeStamp.ofMillis(millis + secondsToAdd * 1000)

  def plusSecondsUnsafe(secondsToAdd: Long): TimeStamp = {
    TimeStamp.ofMillisUnsafe(millis + secondsToAdd * 1000)
  }

  def plusMinutes(minutesToAdd: Long): Option[TimeStamp] =
    TimeStamp.ofMillis(millis + minutesToAdd * 60 * 1000)

  def plusMinutesUnsafe(minutesToAdd: Long): TimeStamp = {
    TimeStamp.ofMillisUnsafe(millis + minutesToAdd * 60 * 1000)
  }

  def plusHours(hoursToAdd: Long): Option[TimeStamp] =
    TimeStamp.ofMillis(millis + hoursToAdd * 60 * 60 * 1000)

  def plusHoursUnsafe(hoursToAdd: Long): TimeStamp =
    TimeStamp.ofMillisUnsafe(millis + hoursToAdd * 60 * 60 * 1000)

  def +(duration: Duration): TimeStamp =
    TimeStamp.ofMillisUnsafe(millis + duration.millis)

  def -(duration: Duration): Option[TimeStamp] =
    TimeStamp.ofMillis(millis - duration.millis)

  def --(another: TimeStamp): Option[Duration] =
    Duration.from(millis - another.millis)

  def isBefore(another: TimeStamp): Boolean =
    millis < another.millis

  def compare(that: TimeStamp): Int = millis compare that.millis
}

object TimeStamp {
  def ofMillisUnsafe(millis: Long): TimeStamp = {
    assume(millis >= 0, "timestamp should be non-negative")
    new TimeStamp(millis)
  }

  def ofMillis(millis: Long): Option[TimeStamp] = {
    if (millis >= 0) Some(new TimeStamp(millis)) else None
  }

  val zero: TimeStamp = ofMillisUnsafe(0)

  def now(): TimeStamp = ofMillisUnsafe(System.currentTimeMillis())
}
