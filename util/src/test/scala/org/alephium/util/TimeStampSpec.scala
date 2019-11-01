package org.alephium.util

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalatest.Assertion

class TimeStampSpec extends AlephiumSpec {
  def invalid(ts: => TimeStamp): Assertion = assertThrows[IllegalArgumentException](ts)

  it should "initialize correctly" in {
    TimeStamp.fromMillis(1).millis is 1
    (TimeStamp.now().millis > 0) is true

    invalid(TimeStamp.fromMillis(-1))
  }

  it should "get current millisecond" in {
    val ts = TimeStamp.now()
    val sys = System.currentTimeMillis()
    (ts.millis + 1000 >= sys) is true
  }

  it should "update correctly" in {
    def check(ts: TimeStamp, it: Instant): Assertion = {
      ts.millis is it.toEpochMilli
    }

    val instant = Instant.now()
    val ts = TimeStamp.fromMillis(instant.toEpochMilli)

    check(ts.plusMillis(100), instant.plusMillis(100))
    check(ts.plusSeconds(100), instant.plusSeconds(100))
    check(ts.plusMinutes(100), instant.plus(100, ChronoUnit.MINUTES))
    check(ts.plusHours(100), instant.plus(100, ChronoUnit.HOURS))
    invalid(ts.plusMillis(- 2 * ts.millis ))
  }
}
