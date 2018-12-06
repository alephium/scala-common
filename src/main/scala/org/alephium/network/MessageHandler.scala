package org.alephium.network

import akka.actor.{ActorRef, Props, Timers}
import org.alephium.constant.Network
import org.alephium.protocol.message._
import org.alephium.storage.BlockPool
import org.alephium.util.BaseActor

import scala.util.Random

object MessageHandler {
  def props(connection: ActorRef, blockPool: ActorRef): Props =
    Props(new MessageHandler(connection, blockPool))

  sealed trait Command
  case object SendPing extends Command
}

class MessageHandler(connection: ActorRef, blockPool: ActorRef) extends BaseActor with Timers {

  override def receive: Receive = handlePayload orElse handleInternal orElse awaitSendPing

  def handlePayload: Receive = {
    case Ping(nonce) =>
      logger.debug("Ping received, response with pong")
      connection ! TcpHandler.envelope(Message(Pong(nonce)))
    case Pong(nonce) =>
      if (nonce == pingNonce) {
        logger.debug("Pong received, no response")
        pingNonce = 0
      } else {
        logger.debug(s"Pong received with wrong nonce: expect $pingNonce, got $nonce")
        context stop self
      }
    case SendBlocks(blocks) =>
      logger.debug(s"Blocks received: $blocks")
      blockPool ! BlockPool.AddBlocks(blocks)
    case GetBlocks(locators) =>
      logger.debug(s"GetBlocks received: $locators")
      blockPool ! BlockPool.GetBlocks(locators)
  }

  def handleInternal: Receive = {
    case BlockPool.SendBlocks(blocks) =>
      connection ! TcpHandler.envelope(Message(SendBlocks(blocks)))
  }

  def awaitSendPing: Receive = {
    case MessageHandler.SendPing =>
      sendPing()
  }

  private var pingNonce: Int = 0

  def sendPing(): Unit = {
    if (pingNonce != 0) {
      logger.debug("No pong message received in time")
      context stop self
    } else {
      pingNonce = Random.nextInt()
      connection ! TcpHandler.envelope(Message(Ping(pingNonce)))
      timers.startSingleTimer(MessageHandler, MessageHandler.SendPing, Network.pingFrequency)
    }
  }
}
