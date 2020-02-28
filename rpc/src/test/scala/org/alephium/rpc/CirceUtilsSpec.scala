package org.alephium.rpc

import java.net.InetSocketAddress

import io.circe.Codec
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.Assertion
import org.scalatest.EitherValues._

import org.alephium.util.{AlephiumSpec, AVector}

class CirceUtilsSpec extends AlephiumSpec {
  import CirceUtils._

  def check[T: Codec](input: T, rawJson: String): Assertion = {
    val json = input.asJson
    printer.print(json) is rawJson
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
}
