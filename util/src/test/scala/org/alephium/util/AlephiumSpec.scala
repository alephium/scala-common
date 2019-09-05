package org.alephium.util

import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbByte
import org.scalactic.Equality
import org.scalactic.source.Position
import org.scalatest.words.ResultOfATypeInvocation
import org.scalatest.{Assertion, FlatSpecLike, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait AlephiumSpec extends FlatSpecLike with ScalaCheckDrivenPropertyChecks with Matchers {

  lazy val bytesGen: Gen[AVector[Byte]] = Gen.listOf(arbByte.arbitrary).map(AVector.from)

  implicit class IsOps[A: Equality](left: A)(implicit pos: Position) {
    // scalastyle:off scalatest-matcher
    def is(right: A): Assertion                             = left shouldEqual right
    def is(right: ResultOfATypeInvocation[_]): Assertion    = left shouldBe right
    def isnot(right: A): Assertion                          = left should not equal right
    def isnot(right: ResultOfATypeInvocation[_]): Assertion = left should not be right
    // scalastyle:on scalatest-matcher
  }
}
