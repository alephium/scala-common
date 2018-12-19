package org.alephium.flow

import java.time.Instant

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow}

import com.typesafe.scalalogging.StrictLogging

import io.circe.parser._

import org.alephium.crypto.ED25519PublicKey
import org.alephium.flow.client.{Miner, Node}
import org.alephium.flow.network.PeerManager
import org.alephium.protocol.model.ChainIndex
import org.alephium.util.Hex._

import scala.concurrent.Future

// scalastyle:off magic.number
trait Platform extends App with StrictLogging {
  def mode: Mode

  def init(): Future[Http.ServerBinding] = {
    val node = mode.createNode

    runServer(node)
  }

  def connect(node: Node, index: Int): Unit = {
    for (peer <- 0 until mode.config.groups if peer != index && peer < index) {
      val remote = mode.index2Ip(peer)
      val until  = Instant.now().plusMillis(mode.config.retryTimeout.toMillis)
      node.peerManager ! PeerManager.Connect(remote, until)
    }
  }

  def runServer(node: Node): Future[Http.ServerBinding] = {
    implicit val system           = node.system
    implicit val materializer     = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val config           = mode.config

    val groups = mode.config.groups
    val from   = mode.config.mainGroup.value

    logger.info(s"index: ${mode.index}, group: $from")

    val route = path("mining") {
      put {
        val publicKey: ED25519PublicKey = ED25519PublicKey.unsafeFrom(
          hex"2db399c90fee96ec2310b62e3f62b5bd87972a96e5fa64675f0adc683546cd1d")

        (0 until groups).foreach { to =>
          val chainIndex = ChainIndex(from, to)
          val props = mode.builders
            .createMiner(publicKey, node, chainIndex)
            .withDispatcher("akka.actor.mining-dispatcher")
          val miner = node.system.actorOf(props, s"Miner-$from-$to")
          miner ! Miner.Start
        }

        complete((StatusCodes.Accepted, "Start mining"))
      }
    } ~
      path("viewer") {
        get {
          handleWebSocketMessages(viewerService(node))
        }
      }

    Http().bindAndHandle(route, "0.0.0.0", mode.httpPort)
  }

  def viewerService(node: Node): Flow[Message, Message, Any] = {
    Flow[Message]
      .collect {
        case requestJson: TextMessage =>
          val request = parse(requestJson.getStrictText)
          val from    = request.flatMap(_.hcursor.get[Long]("from")).toOption.getOrElse(-1L)

          val blocks = node.blockFlow.getHeaders(_.timestamp > from)

          val json = {
            val blocksJson = blocks
              .map { header =>
                node.blockFlow.toJsonUnsafe(header)
              }
              .sorted
              .mkString("[", ",", "]")

            s"""{"blocks":$blocksJson}"""
          }

          TextMessage(json)
      }
  }
}
