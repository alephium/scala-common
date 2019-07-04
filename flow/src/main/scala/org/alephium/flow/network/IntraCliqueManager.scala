package org.alephium.flow.network

import akka.actor.{ActorRef, Props}
import akka.io.Tcp
import org.alephium.flow.PlatformConfig
import org.alephium.flow.network.clique.BrokerHandler
import org.alephium.flow.storage.AllHandlers
import org.alephium.protocol.model.{BrokerInfo, CliqueInfo}
import org.alephium.util.BaseActor

object IntraCliqueManager {
  def props(builder: BrokerHandler.Builder, cliqueInfo: CliqueInfo, allHandlers: AllHandlers)(
      implicit config: PlatformConfig): Props =
    Props(new IntraCliqueManager(builder, cliqueInfo, allHandlers))

  sealed trait Command
  case object GetPeers extends Command

  case class Broker(brokerInfo: BrokerInfo, brokerHandler: ActorRef)

  sealed trait Event
  case object Ready extends Event
}

class IntraCliqueManager(builder: BrokerHandler.Builder,
                         cliqueInfo: CliqueInfo,
                         allHandlers: AllHandlers)(implicit config: PlatformConfig)
    extends BaseActor {

  cliqueInfo.peers.foreachWithIndex {
    case (address, index) =>
      if (index > config.brokerInfo.id) {
        log.debug(s"Connect to broker $index, $address")
        val remoteBroker = BrokerInfo(index, config.groupNumPerBroker, address)
        val props =
          builder.createOutboundBrokerHandler(cliqueInfo, remoteBroker, index, address, allHandlers)
        context.actorOf(props, BaseActor.envalidActorName(s"OutboundBrokerHandler-$address"))
      }
  }

  override def receive: Receive = awaitBrokers(Map.empty)

  // TODO: replace Map with Array for performance
  def awaitBrokers(brokers: Map[Int, (BrokerInfo, ActorRef)]): Receive = {
    case Tcp.Connected(remote, _) =>
      log.debug(s"Connection from $remote")
      val index = cliqueInfo.peers.indexWhere(_ == remote)
      if (index < config.brokerInfo.id) {
        // Note: index == -1 is also the right condition
        log.debug(s"Inbound connection: $remote")
        val name  = BaseActor.envalidActorName(s"InboundBrokerHandler-$remote")
        val props = builder.createInboundBrokerHandler(cliqueInfo, sender(), allHandlers)
        context.actorOf(props, name)
        ()
      }
    case CliqueManager.Connected(cliqueId, brokerInfo) =>
      if (cliqueId == cliqueInfo.id) {
        log.debug(s"Broker connected: $brokerInfo")
        val newBrokers = brokers + (brokerInfo.id -> (brokerInfo -> sender()))
        if (newBrokers.size == cliqueInfo.peers.length - 1) {
          log.debug("All Brokers connected")
          context.parent ! IntraCliqueManager.Ready
          context become handle(newBrokers)
        } else {
          context become awaitBrokers(newBrokers)
        }
      }
  }

  def handle(brokers: Map[Int, (BrokerInfo, ActorRef)]): Receive = {
    case CliqueManager.BroadCastBlock(block, blockMsg, headerMsg, _) =>
      assert(block.chainIndex.relateTo(config.brokerInfo))
      log.debug(s"Broadcasting block/header ${block.chainIndex}")
      brokers.foreach {
        case (_, (info, broker)) =>
          if (block.chainIndex.relateTo(info)) {
            log.debug(s"Send block to broker $info")
            broker ! blockMsg
          } else {
            log.debug(s"Send header to broker $info")
            broker ! headerMsg
          }
      }
  }
}
