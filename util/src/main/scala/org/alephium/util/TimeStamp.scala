package org.alephium.util

class TimeStamp(val millis: Long) extends AnyVal with Ordered[TimeStamp] {
  def plusMillis(millisToAdd: Long): TimeStamp =
    TimeStamp.fromMillis(millis + millisToAdd)

  def plusSeconds(secondsToAdd: Long): TimeStamp =
    TimeStamp.fromMillis(millis + secondsToAdd * 1000)

  def plusMinutes(minutesToAdd: Long): TimeStamp =
    TimeStamp.fromMillis(millis + minutesToAdd * 60 * 1000)

  def plusHours(hoursToAdd: Long): TimeStamp =
    TimeStamp.fromMillis(millis + hoursToAdd * 60 * 60 * 1000)

  def compare(that: TimeStamp): Int = millis compare that.millis
}

object TimeStamp {
  def now(): TimeStamp = fromMillis(System.currentTimeMillis())

  def fromMillis(millis: Long): TimeStamp = {
    require(millis >= 0, "timestamp should be positive")
    new TimeStamp(millis)
  }
}
