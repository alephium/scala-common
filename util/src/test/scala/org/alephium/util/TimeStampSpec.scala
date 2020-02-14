package org.alephium.util

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalacheck.Gen
import org.scalatest.Assertion

class TimeStampSpec extends AlephiumSpec {
  it should "initialize correctly" in {
    TimeStamp.from(1).get.millis is 1
    (TimeStamp.now().millis > 0) is true

    TimeStamp.from(-1).isEmpty is true
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
    val ts      = TimeStamp.unsafeFrom(instant.toEpochMilli)

    check(ts.plusMillis(100).get, instant.plusMillis(100))
    check(ts.plusSeconds(100).get, instant.plusSeconds(100))
    check(ts.plusMinutes(100).get, instant.plus(100, ChronoUnit.MINUTES))
    check(ts.plusHours(100).get, instant.plus(100, ChronoUnit.HOURS))
    ts.plusMillis(-2 * ts.millis).isEmpty is true
  }

  it should "operate correctly" in {
    forAll(Gen.chooseNum(0, Long.MaxValue)) { l0 =>
      forAll(Gen.chooseNum(0, l0)) { l1 =>
        val ts = TimeStamp.unsafeFrom(l0 / 2)
        val dt = Duration.ofMillis(l1 / 2)
        (ts + dt).get.millis is ts.millis + dt.millis
        (ts - dt).get.millis is ts.millis - dt.millis

        val ts0 = TimeStamp.unsafeFrom(l0)
        val ts1 = TimeStamp.unsafeFrom(l1)
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
