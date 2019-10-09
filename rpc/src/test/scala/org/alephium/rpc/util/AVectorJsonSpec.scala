package org.alephium.rpc.util

import org.alephium.util.{AlephiumSpec, AVector}

class AVectorJsonSpec extends AlephiumSpec {
  import AVectorJson._

  "AVectorJson" should "encode and decode" in {
    val encoder = encodeAVector[Int]
    val decoder = decodeAVector[Int]
    forAll { ys: List[Int] =>
      val xs   = AVector.from(ys)
      val json = encoder(xs)
      json.noSpaces is xs.mkString("[", ",", "]")
      decoder.decodeJson(json).right.get is xs
    }
  }
}
