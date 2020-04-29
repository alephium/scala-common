package org.alephium.util

import org.scalacheck.Arbitrary.arbByte
import org.scalacheck.Gen
import org.scalactic.Equality
import org.scalactic.source.Position
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.dsl.ResultOfATypeInvocation
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait AlephiumSpec extends AnyFlatSpecLike with ScalaCheckDrivenPropertyChecks with Matchers {

  lazy val bytesGen: Gen[AVector[Byte]] = Gen.listOf(arbByte.arbitrary).map(AVector.from)

  implicit class IsOps[A: Equality](left: A)(implicit pos: Position) {
    // scalastyle:off scalatest-matcher
    def is(right: A): Assertion                             = left shouldEqual right
    def is(right: ResultOfATypeInvocation[_]): Assertion    = left shouldBe right
    def isnot(right: A): Assertion                          = left should not equal right
    def isnot(right: ResultOfATypeInvocation[_]): Assertion = left should not be right
    // scalastyle:on scalatest-matcher
  }

  implicit class IsEOps[A: Equality, L](left: Either[L, A])(implicit pos: Position) {
    // scalastyle:off scalatest-matcher
    def isE(right: A): Assertion                             = left.toOption.get shouldEqual right
    def isE(right: ResultOfATypeInvocation[_]): Assertion    = left.toOption.get shouldBe right
    def isnotE(right: A): Assertion                          = left.toOption.get should not equal right
    def isnotE(right: ResultOfATypeInvocation[_]): Assertion = left.toOption.get should not be right
    // scalastyle:on scalatest-matcher
  }
}
