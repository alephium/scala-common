package org.alephium.util

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalacheck.Gen
import org.scalatest.Assertion

class TimeStampSpec extends AlephiumSpec {
  def invalid(ts: => TimeStamp): Assertion = assertThrows[IllegalArgumentException](ts)

  it should "initialize correctly" in {
    TimeStamp.fromMillis(1).millis is 1
    (TimeStamp.now().millis > 0) is true

    invalid(TimeStamp.fromMillis(-1))
  }

  it should "get current millisecond" in {
    val ts  = TimeStamp.now()
    val sys = System.currentTimeMillis()
    (ts.millis + 1000 >= sys) is true
  }

  it should "update correctly" in {
    def check(ts: TimeStamp, it: Instant): Assertion = {
      ts.millis is it.toEpochMilli
    }

    val instant = Instant.now()
    val ts      = TimeStamp.fromMillis(instant.toEpochMilli)

    check(ts.plusMillis(100), instant.plusMillis(100))
    check(ts.plusSeconds(100), instant.plusSeconds(100))
    check(ts.plusMinutes(100), instant.plus(100, ChronoUnit.MINUTES))
    check(ts.plusHours(100), instant.plus(100, ChronoUnit.HOURS))
    invalid(ts.plusMillis(-2 * ts.millis))
  }

  it should "operate correctly" in {
    forAll(Gen.chooseNum(0, Long.MaxValue)) { l0 =>
      forAll(Gen.chooseNum(0, l0)) { l1 =>
        val ts = TimeStamp.fromMillis(l0 / 2)
        val dt = Duration.ofMillis(l1 / 2)
        (ts + dt).millis is ts.millis + dt.millis
        (ts - dt).millis is ts.millis - dt.millis

        val ts0 = TimeStamp.fromMillis(l0)
        val ts1 = TimeStamp.fromMillis(l1)
        (ts0 diff ts1).millis is l0 - l1
        if (l0 != l1) {
          ts0 isBefore ts1 is false
          ts1 isBefore ts0 is true
        } else {
          ts0 isBefore ts1 is false
          ts1 isBefore ts0 is false
        }
      }
    }
  }
}
