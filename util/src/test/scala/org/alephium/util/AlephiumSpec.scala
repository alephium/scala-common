package org.alephium.util

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalactic.Equality
import org.scalactic.source.Position
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.dsl.ResultOfATypeInvocation
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait AlephiumSpec extends AnyFlatSpecLike with ScalaCheckDrivenPropertyChecks with Matchers {

  implicit lazy val bytesGen: Arbitrary[AVector[Byte]] = Arbitrary(
    arbitrary[List[Byte]].map(AVector.from))
  implicit lazy val i32Gen: Arbitrary[I32] = Arbitrary(arbitrary[Int].map(I32.unsafe))
  implicit lazy val u32Gen: Arbitrary[U32] = Arbitrary(arbitrary[Int].map(U32.unsafe))
  implicit lazy val i64Gen: Arbitrary[I64] = Arbitrary(arbitrary[Long].map(I64.unsafe))
  implicit lazy val u64Gen: Arbitrary[U64] = Arbitrary(arbitrary[Long].map(U64.unsafe))

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

  import java.math.BigInteger
  implicit class BigIntegerWrapper(val n: BigInteger) extends Ordered[BigIntegerWrapper] {
    override def compare(that: BigIntegerWrapper): Int = this.n.compareTo(that.n)
  }
}
