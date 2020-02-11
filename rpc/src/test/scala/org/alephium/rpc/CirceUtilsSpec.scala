package org.alephium.rpc

import io.circe.syntax._

import org.alephium.util.{AlephiumSpec, AVector}

class CirceUtilsSpec extends AlephiumSpec {
  import CirceUtils._

  "AVectorJson" should "encode and decode" in {
    implicit val codec = avectorCodec[Int]
    forAll { ys: List[Int] =>
      val xs = AVector.from(ys)
      printer.print(xs.asJson) is xs.mkString("[", ",", "]")
      xs.asJson.as[AVector[Int]].right.get is xs
    }
  }
}
