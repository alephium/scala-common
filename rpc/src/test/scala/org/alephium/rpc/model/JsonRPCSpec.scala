package org.alephium.rpc.model

import scala.concurrent.Future
import scala.util.Success

import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.EitherValues

import org.alephium.util.AlephiumSpec

class JsonRPCSpec extends AlephiumSpec with EitherValues {

  val dummy = Future.successful(JsonRPC.Response.Success(Json.Null, 0))

  def handler(method: String): JsonRPC.Handler = Map((method, (_: JsonRPC.Request) => dummy))

  def parseRequest(jsonRaw: String): JsonRPC.RequestUnsafe = {
    val json = parse(jsonRaw).right.value
    json.as[JsonRPC.RequestUnsafe].right.value
  }

  it should "encode request" in {
    val request = JsonRPC.Request("foobar", Some(Json.fromInt(42)), 1)
    request.asJson.noSpaces is """{"jsonrpc":"2.0","method":"foobar","params":42,"id":1}"""
  }

  it should "encode request - drop nulls " in {
    val request = JsonRPC.Request("foobar", Some(Json.Null), 1)
    request.asJson.noSpaces is """{"jsonrpc":"2.0","method":"foobar","id":1}"""
  }

  it should "encode notification" in {
    val notification = JsonRPC.Notification("foobar", Some(Json.fromInt(42)))
    notification.asJson.noSpaces is """{"jsonrpc":"2.0","method":"foobar","params":42}"""
  }

  it should "encode notification - drop nulls" in {
    val notification = JsonRPC.Notification("foobar", Some(Json.Null))
    notification.asJson.noSpaces is """{"jsonrpc":"2.0","method":"foobar"}"""
  }

  it should "encode response - success" in {
    val success: JsonRPC.Response = JsonRPC.Response.Success(Json.fromInt(42), 1)
    success.asJson.noSpaces is """{"jsonrpc":"2.0","result":42,"id":1}"""
  }

  it should "encode response - success - drop nulls" in {
    val success: JsonRPC.Response = JsonRPC.Response.Success(Json.Null, 1)
    success.asJson.noSpaces is """{"jsonrpc":"2.0","id":1}"""
  }

  it should "encode response - failure" in {
    val failure: JsonRPC.Response = JsonRPC.Response.Failure(JsonRPC.Error.InvalidRequest, Some(1))
    failure.asJson.noSpaces is """{"jsonrpc":"2.0","error":{"code":-32600,"message":"The request is invalid."},"id":1}"""
  }

  it should "encode response - failure - no id" in {
    val failure: JsonRPC.Response = JsonRPC.Response.Failure(JsonRPC.Error.InvalidRequest, None)
    failure.asJson.noSpaces is """{"jsonrpc":"2.0","error":{"code":-32600,"message":"The request is invalid."}}"""
  }

  it should "parse notification" in {
    val jsonRaw = """{"jsonrpc": "2.0", "method": "foobar"}"""
    val json = parse(jsonRaw).right.value
    val notification = json.as[JsonRPC.NotificationUnsafe].right.value.asNotification.right.value
    notification.method is "foobar"
  }

  it should "parse notification - fail on wrong rpc version" in {
    val jsonRaw = """{"jsonrpc": "1.0", "method": "foobar"}"""
    val json = parse(jsonRaw).right.value
    val error = json.as[JsonRPC.NotificationUnsafe].right.value.asNotification.left.value
    error is JsonRPC.Error.InvalidRequest
  }

  it should "parse request with no params" in {
    val request = parseRequest("""{"jsonrpc": "2.0", "method": "foobar", "id": 1}""")

    request.jsonrpc is JsonRPC.version
    request.method is "foobar"
    request.params is None
    request.id is 1

    request.runWith(handler("foobar")) is dummy
  }

  it should "parse request with params" in {
    val request = parseRequest("""{"jsonrpc": "2.0", "method": "foobar", "params": 42, "id": 1}""")

    request.jsonrpc is JsonRPC.version
    request.method is "foobar"
    request.params is Some(Json.fromLong(42))
    request.id is 1

    request.runWith(handler("foobar")) is dummy
  }

  it should "parse request - fail on wrong rpc version" in {
    val request = parseRequest("""{"jsonrpc": "1.0", "method": "foobar", "id": 1}""")

    request.jsonrpc is "1.0"

    val result = request.runWith(handler("foobar"))
    result.value is Some(Success(JsonRPC.Response.Failure(JsonRPC.Error.InvalidRequest, Some(1))))
  }

  it should "parse response - success" in {
    val jsonRaw = """{"jsonrpc": "2.0", "result": 42, "id": 1}"""
    val success = parse(jsonRaw).right.value.as[JsonRPC.Response.Success].right.value

    success.result is Json.fromInt(42)
    success.id is 1
  }

  it should "parse response - success - fail on wrong rpc version" in {
    val jsonRaw = """{"jsonrpc": "1.0", "result": 42, "id": 1}"""
    val error = parse(jsonRaw).right.value.as[JsonRPC.Response.Success].left.value
    error.message is "Invalid JSONRPC version '1.0'."
  }
}
