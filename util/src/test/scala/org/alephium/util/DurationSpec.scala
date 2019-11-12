package org.alephium.util

import java.time.{Duration => JDuration}

import scala.concurrent.duration.{FiniteDuration => SDuration, MILLISECONDS}

import org.scalatest.Assertion

class DurationSpec extends AlephiumSpec {
  def check(dt: Duration, jdt: JDuration): Assertion = {
    dt.millis is jdt.toMillis
    dt.toSeconds is jdt.getSeconds
    dt.toMinutes is jdt.toMinutes
    dt.toHours is jdt.toHours

    if (dt.millis <= Long.MaxValue / 2 && dt.millis >= Long.MaxValue / 2) {
      (dt + dt).millis is 2 * dt.millis
      (dt * 2).millis is 2 * dt.millis
    }
    (dt - dt).millis is 0
    (dt / 2).millis is dt.millis / 2
  }

  it should "initialize correctly" in {
    Duration.zero.millis is 0
    forAll { millis: Long =>
      whenever(millis > Long.MinValue) { // otherwise it will fail in jdk8!
        JDuration.ofMillis(millis).toMillis
        check(Duration.ofMillis(millis), JDuration.ofMillis(millis))

        val seconds = millis / 1000
        check(Duration.ofSeconds(seconds), JDuration.ofSeconds(seconds))

        val minutes = seconds / 60
        check(Duration.ofMinutes(minutes), JDuration.ofMinutes(minutes))

        val hours = minutes / 60
        check(Duration.ofHours(hours), JDuration.ofHours(hours))
      }
    }
  }

  it should "operate correctly" in {
    forAll { (l0: Long, l1: Long) =>
      val dt0 = Duration.ofMillis(l0 / 2)
      val dt1 = Duration.ofMillis(l1 / 2)
      (dt0 + dt1).millis is dt0.millis + dt1.millis
      (dt0 - dt1).millis is dt0.millis - dt1.millis
      (dt0 * 2).millis is dt0.millis * 2
      (dt0 / 2).millis is dt0.millis / 2

      val maxMS = Long.MaxValue / 1000000
      if (l0 >= -maxMS && l0 <= maxMS) {
        Duration.ofMillis(l0).asScala is SDuration.apply(l0, MILLISECONDS)
      } else {
        assertThrows[IllegalArgumentException](Duration.ofMillis(l0).asScala)
      }
    }
  }
}
