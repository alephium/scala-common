package org.alephium.rpc

import akka.util.ByteString
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import org.scalatest.{EitherValues}

import org.alephium.util.AlephiumSpec

case class Foo(bar: ByteString)
object Foo extends JsonRPCSupport {
  implicit val decoder: Decoder[Foo] = deriveDecoder[Foo]
  implicit val encoder: Encoder[Foo] = deriveEncoder[Foo]
}

class JsonRPCSupportSpec extends AlephiumSpec with EitherValues {
  it should "encode/decode hexstring" in {
    val jsonRaw = """{"bar": "48656c6c6f20776f726c642021"}"""
    val json    = parse(jsonRaw).right.value
    val foo     = json.as[Foo].right.value
    foo.bar.utf8String is "Hello world !"
  }
}
