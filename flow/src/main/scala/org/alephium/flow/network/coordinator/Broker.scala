package org.alephium.flow.network.coordinator

import java.time.Instant

import akka.actor.{ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import org.alephium.flow.PlatformConfig
import org.alephium.util.BaseActor

import scala.concurrent.duration._

object Broker {
  def props()(implicit config: PlatformConfig): Props = Props(new Broker())

  sealed trait Command
  case object Retry extends Command
}

class Broker()(implicit config: PlatformConfig) extends BaseActor {
  IO(Tcp)(context.system) ! Tcp.Connect(config.masterAddress)

  def until: Instant = Instant.now().plusMillis(config.retryTimeout.toMillis)

  override def receive: Receive = awaitMaster(until)

  def awaitMaster(until: Instant): Receive = {
    case Broker.Retry =>
      IO(Tcp)(context.system) ! Tcp.Connect(config.masterAddress)

    case _: Tcp.Connected =>
      log.debug(s"Connected to master: ${config.masterAddress}")
      val connection = sender()
      connection ! Tcp.Register(self)
      val info = BrokerConnector.BrokerInfo(config.brokerId, config.publicAddress)
      connection ! BrokerConnector.envolop(info)
      context become awaitCliqueInfo(connection, ByteString.empty)

    case Tcp.CommandFailed(c: Tcp.Connect) =>
      val current = Instant.now()
      if (current isBefore until) {
        scheduleOnce(self, Broker.Retry, 1.second)
      } else {
        log.info(s"Cannot connect to ${c.remoteAddress}")
        context stop self
      }
  }

  def awaitCliqueInfo(connection: ActorRef, unaligned: ByteString): Receive = {
    case Tcp.Received(data) =>
      BrokerConnector.deserializeCliqueInfo(unaligned ++ data) match {
        case Right(Some((cliqueInfo, _))) =>
          connection ! BrokerConnector.Ready
          context.parent ! cliqueInfo
          context.stop(self)
        case Right(None) =>
          context become awaitCliqueInfo(connection, unaligned ++ data)
        case Left(e) =>
          log.info(s"Received corrupted data; error: ${e.toString}; Closing connection")
          context stop self
      }
  }
}
