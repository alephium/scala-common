package org.alephium.flow.storage

import akka.actor.{ActorRef, ActorSystem}
import org.alephium.flow.PlatformConfig
import org.alephium.protocol.model.ChainIndex

/*
 * @globalHandlers: actor of BlockHandler
 * @poolHandlers: actors of BlockPoolHandler
 */
case class AllHandlers(flowHandler: ActorRef,
                       blockHandlers: Map[ChainIndex, ActorRef],
                       headerHandlers: Map[ChainIndex, ActorRef])(implicit config: PlatformConfig) {

  def getBlockHandler(chainIndex: ChainIndex): ActorRef = {
    assert(chainIndex.relateTo(config.brokerId))
    blockHandlers(chainIndex)
  }

  def getHeaderHandler(chainIndex: ChainIndex): ActorRef = {
    assert(!chainIndex.relateTo(config.brokerId))
    headerHandlers(chainIndex)
  }
}

object AllHandlers {
  def build(system: ActorSystem, cliqueManager: ActorRef, blockFlow: BlockFlow)(
      implicit config: PlatformConfig): AllHandlers = {
    val flowProps =
      FlowHandler.props(blockFlow).withDispatcher("akka.actor.flow-handler-dispatcher")
    val flowHandler    = system.actorOf(flowProps, "FlowHandler")
    val blockHandlers  = buildBlockHandlers(system, cliqueManager, blockFlow, flowHandler)
    val headerHandlers = buildHeaderHandlers(system, blockFlow, flowHandler)
    AllHandlers(flowHandler, blockHandlers, headerHandlers)
  }

  private def buildBlockHandlers(
      system: ActorSystem,
      cliqueManager: ActorRef,
      blockFlow: BlockFlow,
      flowHandler: ActorRef)(implicit config: PlatformConfig): Map[ChainIndex, ActorRef] = {
    val handlers = for {
      from <- 0 until config.groups
      to   <- 0 until config.groups
      chainIndex = ChainIndex(from, to)
      if chainIndex.relateTo(config.brokerId)
    } yield {
      val handler = system.actorOf(
        BlockChainHandler.props(blockFlow, chainIndex, cliqueManager, flowHandler),
        s"BlockChainHandler-$from-$to")
      chainIndex -> handler
    }
    handlers.toMap
  }

  private def buildHeaderHandlers(system: ActorSystem, blockFlow: BlockFlow, flowHandler: ActorRef)(
      implicit config: PlatformConfig): Map[ChainIndex, ActorRef] = {
    val headerHandlers = for {
      from <- 0 until config.groups
      to   <- 0 until config.groups
      chainIndex = ChainIndex(from, to)
      if !chainIndex.relateTo(config.brokerId)
    } yield {
      val headerHander = system.actorOf(
        HeaderChainHandler.props(blockFlow, chainIndex, flowHandler),
        s"HeaderChainHandler-$from-$to")
      chainIndex -> headerHander
    }
    headerHandlers.toMap
  }
}
