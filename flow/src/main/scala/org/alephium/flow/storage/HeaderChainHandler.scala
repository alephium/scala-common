package org.alephium.flow.storage

import akka.actor.{ActorRef, Props}
import org.alephium.flow.PlatformConfig
import org.alephium.flow.model.DataOrigin
import org.alephium.flow.network.{PeerManager, TcpHandler}
import org.alephium.protocol.message.SendHeaders
import org.alephium.protocol.model.{BlockHeader, ChainIndex}
import org.alephium.util.{AVector, BaseActor}

object HeaderChainHandler {
  def props(blockFlow: BlockFlow,
            chainIndex: ChainIndex,
            peerManager: ActorRef,
            flowHandler: ActorRef)(implicit config: PlatformConfig): Props =
    Props(new HeaderChainHandler(blockFlow, chainIndex, peerManager, flowHandler))

  sealed trait Command
  case class AddHeaders(headers: AVector[BlockHeader], origin: DataOrigin)
}

class HeaderChainHandler(val blockFlow: BlockFlow,
                         val chainIndex: ChainIndex,
                         peerManager: ActorRef,
                         flowHandler: ActorRef)(implicit val config: PlatformConfig)
    extends BaseActor
    with ChainHandlerLogger {
  import HeaderChainHandler._

  val chain: BlockHeaderPool = blockFlow.getHeaderChain(chainIndex)

  override def receive: Receive = {
    case AddHeaders(headers, origin) =>
      // TODO: support more heads later
      assert(headers.length == 1)
      val header = headers.head
      handleHeader(header, origin)
  }

  def handleHeader(header: BlockHeader, origin: DataOrigin): Unit = {
    if (blockFlow.contains(header)) {
      log.debug(s"Header already existed")
    } else {
      blockFlow.validate(header, fromBlock = false) match {
        case Left(e) =>
          log.debug(s"Failed in header validation: ${e.toString}")
        case Right(_) =>
          logInfo(header)
          broadcast(header, origin)
          flowHandler.tell(FlowHandler.AddHeader(header), sender())
      }
    }
  }

  def broadcast(header: BlockHeader, origin: DataOrigin): Unit = {
    val headerMsg = TcpHandler.envelope(SendHeaders(AVector(header)))
    peerManager ! PeerManager.BroadCastHeader(header, headerMsg, origin)
  }
}
