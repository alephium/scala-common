package org.alephium.rpc

import akka.util.ByteString
import io.circe._

import org.alephium.util.Hex

trait JsonRPCSupport {
  implicit val byteStringEncoder: Encoder[ByteString] = new Encoder[ByteString] {
    def apply(bs: ByteString): Json = Json.fromString(Hex.toHexString(bs.toIndexedSeq))
  }

  implicit val byteStringDecoder: Decoder[ByteString] = new Decoder[ByteString] {
    def apply(c: HCursor): Decoder.Result[ByteString] = c.as[String].map(Hex.unsafeFrom(_))
  }
}

object JsonRPCSupport extends JsonRPCSupport {}
