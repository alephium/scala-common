package org.alephium.flow.client

import akka.actor.{ActorRef, Props}
import akka.util.ByteString
import org.alephium.crypto.ED25519PublicKey
import org.alephium.flow.PlatformConfig
import org.alephium.flow.client.Miner.BlockAdded
import org.alephium.flow.model.BlockTemplate
import org.alephium.flow.model.DataOrigin.LocalMining
import org.alephium.flow.storage.FlowHandler.BlockFlowTemplate
import org.alephium.flow.storage.BlockChainHandler
import org.alephium.protocol.config.GroupConfig
import org.alephium.protocol.model._
import org.alephium.util.{AVector, BaseActor}

import scala.annotation.tailrec
import scala.util.Random

object FairMiner {
  def props(node: Node)(implicit config: PlatformConfig): Props = {
    val addresses = AVector.tabulate(config.groups) { i =>
      val index          = GroupIndex(i)
      val (_, publicKey) = GroupConfig.generateKeyForGroup(index)
      publicKey
    }
    props(addresses, node)
  }

  def props(addresses: AVector[ED25519PublicKey], node: Node)(
      implicit config: PlatformConfig): Props = {
    require(addresses.length == config.groups)
    addresses.foreachWithIndex { (address, i) =>
      require(GroupIndex.from(address).value == i)
    }
    Props(new FairMiner(addresses, node))
  }

  sealed trait Command
  case class MiningResult(
      blockOpt: Option[Block],
      chainIndex: ChainIndex,
      miningCount: BigInt,
      template: BlockTemplate
  ) extends Command
}

class FairMiner(addresses: AVector[ED25519PublicKey], node: Node)(
    implicit val config: PlatformConfig)
    extends BaseActor
    with FairMinerState {
  val handlers = node.allHandlers
  val actualMiners: AVector[AVector[ActorRef]] =
    AVector.tabulate(config.groupNumPerBroker, config.groups) {
      case (shift, to) =>
        val from = config.groupFrom + shift
        val props = ActualMiner
          .props(ChainIndex(from, to))
          .withDispatcher("akka.actor.mining-dispatcher")
        context.actorOf(props)
    }

  def receive: Receive = awaitStart

  def awaitStart: Receive = {
    case Miner.Start =>
      log.info("Start mining")
      initialize()
      context become (handleMining orElse awaitStop)
  }

  def awaitStop: Receive = {
    case Miner.Stop =>
      log.info("Stop mining")
      context become awaitStart
  }

  def handleMining: Receive = {
    case FairMiner.MiningResult(blockOpt, chainIndex, miningCount, template) =>
      assert(config.brokerId.contains(chainIndex.from))
      val fromShift = chainIndex.from.value - config.groupFrom
      val to        = chainIndex.to.value
      increaseCounts(fromShift, to, miningCount)
      blockOpt match {
        case Some(block) =>
          val miningCount = getMiningCount(fromShift, to)
          log.debug(s"MiningCounts: $countsToString")
          log.info(
            s"A new block ${block.shortHex} got mined for $chainIndex, miningCount: $miningCount, target: ${block.header.target}")
          val blockHandler = node.allHandlers.getBlockHandler(chainIndex)
          blockHandler ! BlockChainHandler.AddBlocks(AVector(block), LocalMining)
        case None =>
          refreshLastTask(fromShift, to, template)
      }
    case BlockAdded(chainIndex) =>
      assert(config.brokerId.contains(chainIndex.from))
      val fromShift = chainIndex.from.value - config.groupFrom
      val to        = chainIndex.to.value
      prepareTemplate(fromShift, to)
  }

  def getBlockTemplate(flowTemplate: BlockFlowTemplate): BlockTemplate = {
    assert(config.brokerId.contains(flowTemplate.index.from))
    val address = addresses(flowTemplate.index.to.value)
    // scalastyle:off magic.number
    val data         = ByteString.fromInts(Random.nextInt())
    val transactions = AVector.tabulate(1000)(Transaction.coinbase(address, _, data))
    // scalastyle:on magic.number
    BlockTemplate(flowTemplate.deps, flowTemplate.target, transactions)
  }

  def prepareTemplate(fromShift: Int, to: Int): Unit = {
    assert(0 <= fromShift && fromShift < config.groupNumPerBroker && 0 <= to && to < config.groups)
    val index         = ChainIndex(config.groupFrom + fromShift, to)
    val flowTemplate  = node.blockFlow.prepareBlockFlowUnsafe(index)
    val blockTemplate = getBlockTemplate(flowTemplate)
    addNewTask(fromShift, to, blockTemplate)
  }

  def startTask(from: Int, to: Int, template: BlockTemplate): Unit = {
    actualMiners(from)(to) ! ActualMiner.Task(template)
  }
}

object ActualMiner {
  def props(index: ChainIndex)(implicit config: PlatformConfig): Props =
    Props(new ActualMiner(index))

  sealed trait Command
  case class Task(template: BlockTemplate) extends Command
}

class ActualMiner(index: ChainIndex)(implicit config: PlatformConfig) extends BaseActor {
  import ActualMiner._

  override def receive: Receive = {
    case Task(template) =>
      mine(template) match {
        case Some((block, miningCount)) =>
          context.sender() ! FairMiner.MiningResult(Some(block), index, miningCount, template)
        case None =>
          context.sender() ! FairMiner.MiningResult(None, index, config.nonceStep, template)
      }
  }

  def mine(template: BlockTemplate): Option[(Block, BigInt)] = {
    val nonceStart = BigInt(Random.nextInt(Integer.MAX_VALUE))
    val nonceEnd   = nonceStart + config.nonceStep

    @tailrec
    def iter(current: BigInt): Option[(Block, BigInt)] = {
      if (current < nonceEnd) {
        val header = template.buildHeader(current)
        if (header.preValidate(index))
          Some((Block(header, template.transactions), current - nonceStart + 1))
        else iter(current + 1)
      } else None
    }
    iter(nonceStart)
  }
}
