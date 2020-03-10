package org.alephium.rpc

import scala.concurrent.Future

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.{Json, JsonObject}
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.EitherValues

import org.alephium.rpc.model._
import org.alephium.util.AlephiumSpec

class JsonRPCHandlerSpec extends AlephiumSpec with ScalatestRouteTest with EitherValues {
  it should "route http" in {
    val response = JsonRPC.Response.Success(Json.fromInt(42), 1)

    val handler = Map("foo" -> { _: JsonRPC.Request =>
      Future.successful(response)
    })

    val route = JsonRPCHandler.routeHttp(handler)

    val jsonRequest = CirceUtils.print(JsonRPC.Request("foo", JsonObject.empty.asJson, 1).asJson)
    val httpRequest = HttpRequest(HttpMethods.POST,
                                  "/",
                                  entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

    httpRequest ~> route ~> check {
      status.intValue is 200
      contentType is MediaTypes.`application/json`
      val text    = responseAs[String]
      val json    = parse(text).right.value
      val success = json.as[JsonRPC.Response.Success].right.value

      text is """{"jsonrpc":"2.0","result":42,"id":1}"""
      success is response
    }
  }
}
