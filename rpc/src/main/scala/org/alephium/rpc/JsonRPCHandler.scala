package org.alephium.rpc

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.syntax._
import model.JsonRPC._

object JsonRPCHandler extends StrictLogging {
  // TODO Introduce `JsonRPCConfig` and move this there.
  val websocketStreamTimeout = 250.milliseconds

  def failure(error: Error): Response = Response.Failure(Json.Null, error)

  def handleRequest(handler: Handler, json: Json): Future[Response] =
    json.as[RequestUnsafe] match {
      case Right(requestUnsafe) =>
        requestUnsafe.validate(handler) match {
          case Right((request, f)) => f(request)
          case Left(error)    => Future.successful(requestUnsafe.failure(error))
        }
      case Left(decodingFailure) =>
        logger.debug(s"Unable to decode JSON-RPC request. (${decodingFailure})")
        Future.successful(failure(Error.InvalidRequest))
    }

  def handleWebSocketRPC(handler: Handler)(implicit EC: ExecutionContext,
                                           FM: Materializer): Flow[Message, Message, Any] = {
    def handleText(text: String) =
      handleRequest(handler, text.asJson).map { response =>
        TextMessage(response.asJson.toString)
      }

    Flow[Message].mapAsync(1) {
      case TextMessage.Strict(text) => handleText(text)
      case message @ TextMessage.Streamed(_) =>
        message.toStrict(websocketStreamTimeout).flatMap(strict => handleText(strict.text))
      case message =>
        logger.debug(
          s"Unsupported binary web socket message received, was expecting JSON-RPC text message. (${message})")
        Future.successful(TextMessage(failure(Error.InternalError).asJson.toString))
    }
  }

  def routeWs(handler: Handler)(implicit EC: ExecutionContext, FM: Materializer): Route =
    get {
      handleWebSocketMessages(handleWebSocketRPC(handler))
    }

  def routeHttp(handler: Handler): Route =
    post {
      entity(as[Json]) { json =>
        onSuccess(handleRequest(handler, json)) { response =>
          complete(response.asJson.toString)
        }
      }
    }
}
