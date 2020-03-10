package org.alephium.rpc

import java.net.InetSocketAddress

import akka.util.ByteString
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.Assertion
import org.scalatest.EitherValues._

import org.alephium.util.{AlephiumSpec, AVector}

case class Foo(bar: ByteString)
object Foo {
  import CirceUtils._
  implicit val decoder: Decoder[Foo] = deriveDecoder[Foo]
  implicit val encoder: Encoder[Foo] = deriveEncoder[Foo]
}

class CirceUtilsSpec extends AlephiumSpec {
  import CirceUtils._

  def check[T: Codec](input: T, rawJson: String): Assertion = {
    val json = input.asJson
    print(json) is rawJson
    json.as[T].right.get is input
  }

  it should "encode/decode vectors" in {
    forAll { ys: List[Int] =>
      check(AVector.from(ys), ys.mkString("[", ",", "]"))
    }
  }

  def addressJson(addr: String): String = s"""{"addr":"$addr","port":9000}"""

  it should "encode/decode socket addresses" in {
    val addr0    = "127.0.0.1"
    val address0 = new InetSocketAddress(addr0, 9000)
    check(address0, addressJson(addr0))

    val addr1    = "2001:db8:85a3:0:0:8a2e:370:7334"
    val address1 = new InetSocketAddress(addr1, 9000)
    check(address1, addressJson(addr1))
  }

  it should "fail for address based on host name" in {
    val rawJson = addressJson("foobar")
    parse(rawJson).right.value.as[InetSocketAddress].isLeft is true
  }

  it should "encode/decode hexstring" in {
    val jsonRaw = """{"bar": "48656c6c6f20776f726c642021"}"""
    val json    = parse(jsonRaw).right.value
    val foo     = json.as[Foo].right.value
    foo.bar.utf8String is "Hello world !"
  }
}
