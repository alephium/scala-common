package org.alephium.rpc.model

import io.circe.parser._
import org.scalatest.EitherValues

import org.alephium.util.AlephiumSpec

class JsonRPCSpec extends AlephiumSpec with EitherValues {
  it should "parse notification" in {
    val jsonRaw = """{"jsonrpc": "2.0", "method": "foobar"}"""
    val json = parse(jsonRaw).right.value
    val notification = json.as[JsonRPC.Notification].right.value
    notification.method is "foobar"
  }

  it should "parse notification - fail on wrong rpc version" in {
    val jsonRaw = """{"jsonrpc": "1.0", "method": "foobar"}"""
    val json = parse(jsonRaw).right.value
    val error = json.as[JsonRPC.Notification].left.value
    error.message is "Invalid JSONRPC version '1.0'."
  }
}
