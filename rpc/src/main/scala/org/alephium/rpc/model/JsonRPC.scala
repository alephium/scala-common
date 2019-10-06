package org.alephium.rpc.model

import scala.concurrent.Future

import com.typesafe.scalalogging.StrictLogging
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

/* Ref: https://www.jsonrpc.org/specification
 *
 * The only difference is that the type for response we use here is Option[Long]
 */

object JsonRPC extends StrictLogging {
  type Handler = Map[String, Request => Future[Response]]

  val versionKey = "jsonrpc"
  val version    = "2.0"

  def versionSet(json: Json): Json = json.mapObject(_.+:(versionKey -> Json.fromString(version)))

  case class Error(code: Int, message: String)
  object Error {
    // scalastyle:off magic.number
    val ParseError     = Error(-32700, "Unable to parse request.")
    val InvalidRequest = Error(-32600, "The request is invalid.")
    val MethodNotFound = Error(-32601, "Method not found.")
    val InvalidParams  = Error(-32602, "Invalid parameters.")
    val InternalError  = Error(-32603, "Internal error.")
    // scalastyle:on
  }

  trait WithId { def id: Long }

  case class RequestUnsafe(
      jsonrpc: String,
      method: String,
      params: Json,
      id: Long
  ) extends WithId {
    def runWith(handler: Handler): Future[Response] = {
      if (jsonrpc == JsonRPC.version) {
        handler.get(method) match {
          case Some(f) => f(Request(method, params, id))
          case None    => Future.successful(Response.failed(this, Error.MethodNotFound))
        }
      } else {
        Future.successful(Response.failed(this, Error.InvalidRequest))
      }
    }
  }
  object RequestUnsafe {
    implicit val decoder: Decoder[RequestUnsafe] = deriveDecoder[RequestUnsafe]
  }

  case class Request(method: String, params: Json, id: Long) extends WithId {
    def paramsAs[A: Decoder]: Either[Response, A] = params.as[A] match {
      case Right(a) => Right(a)
      case Left(decodingFailure) =>
        logger.debug(
          s"Unable to decode JsonRPC request parameters. ($method@$id: $decodingFailure)")
        Left(Response.failed(this, Error.InvalidParams))
    }
  }
  object Request {
    implicit val encoder: Encoder[Request] = deriveEncoder[Request].mapJson(versionSet)
  }

  case class Notification(method: String, params: Json)
  object Notification {
    implicit val encoder: Encoder[Notification] = deriveEncoder[Notification].mapJson(versionSet)
  }

  sealed trait Response
  object Response {
    def successful[T <: WithId](request: T): Response               = Success(request.id, Json.True)
    def successful[T <: WithId](request: T, result: Json): Response = Success(request.id, result)
    def failed[T <: WithId](request: T, error: Error): Response     = Failure(Some(request.id), error)
    def failed(error: Error): Response                              = Failure(None, error)

    case class Success(id: Long, result: Json) extends Response
    object Success {
      implicit val encoder: Encoder[Success] = deriveEncoder[Success]
    }
    case class Failure(id: Option[Long], error: Error) extends Response
    object Failure {
      import io.circe.generic.auto._ // Note: I hate this!
      implicit val encoder: Encoder[Failure] = deriveEncoder[Failure]
    }

    implicit val encoder: Encoder[Response] = {
      val product: Encoder[Response] = Encoder.instance {
        case x @ Success(_, _) => x.asJson
        case x @ Failure(_, _) => x.asJson
      }
      product.mapJson(versionSet)
    }
  }
}
