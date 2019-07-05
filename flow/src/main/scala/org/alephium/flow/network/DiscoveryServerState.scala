package org.alephium.flow.network

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.io.Udp
import org.alephium.protocol.config.{DiscoveryConfig, GroupConfig}
import org.alephium.protocol.message.DiscoveryMessage
import org.alephium.protocol.message.DiscoveryMessage._
import org.alephium.protocol.model.{CliqueId, CliqueInfo}
import org.alephium.util.AVector

import scala.collection.mutable

trait DiscoveryServerState {
  implicit def config: GroupConfig with DiscoveryConfig
  def log: LoggingAdapter

  def bootstrap: AVector[InetSocketAddress]
  def selfCliqueInfo: CliqueInfo

  import DiscoveryServer._

  private var socket: ActorRef = _
  protected val table          = mutable.HashMap.empty[CliqueId, PeerStatus]
  private val pendings         = mutable.HashMap.empty[CliqueId, AwaitPong]
  private val pendingMax       = 2 * config.groups * config.neighborsPerGroup

  def setSocket(s: ActorRef): Unit = {
    socket = s
  }

  def getActivePeers: AVector[CliqueInfo] = {
    AVector.from(table.values.map(_.info))
  }

  def getPeersNum: Int = table.size

  def getNeighbors(target: CliqueId): AVector[CliqueInfo] = {
    val candidates = if (target == selfCliqueInfo.id) {
      AVector.from(table.values.map(_.info))
    } else {
      AVector.from(table.values.map(_.info).filter(_.id != target)) :+ selfCliqueInfo
    }
    candidates.sortBy(info => target.hammingDist(info.id)).takeUpto(config.neighborsPerGroup)
  }

  def isInTable(cliqueId: CliqueId): Boolean = {
    table.contains(cliqueId)
  }

  def isPending(cliqueId: CliqueId): Boolean = {
    pendings.contains(cliqueId)
  }

  def isUnknown(cliqueId: CliqueId): Boolean = !isInTable(cliqueId) && !isPending(cliqueId)

  def isPendingAvailable: Boolean = pendings.size < pendingMax

  def getPeer(cliqueId: CliqueId): Option[CliqueInfo] = {
    table.get(cliqueId).map(_.info)
  }

  def getPending(cliqueId: CliqueId): Option[AwaitPong] = {
    pendings.get(cliqueId)
  }

  def updateStatus(cliqueId: CliqueId): Unit = {
    table.get(cliqueId) match {
      case Some(status) =>
        table(cliqueId) = status.copy(updateAt = System.currentTimeMillis())
      case None => ()
    }
  }

  def getPendingStatus(cliqueId: CliqueId): Option[AwaitPong] = {
    pendings.get(cliqueId)
  }

  def cleanup(): Unit = {
    val now = System.currentTimeMillis()
    val toRemove = table.values
      .filter(status => now - status.updateAt > config.peersTimeout.toMillis)
      .map(_.info.id)
      .toSet
    table --= toRemove

    val deadPendings = pendings.collect {
      case (cliqueId, status) if now - status.pingAt > config.peersTimeout.toMillis => cliqueId
    }
    pendings --= deadPendings
  }

  private def appendPeer(cliqueInfo: CliqueInfo): Unit = {
    log.debug(s"Adding a new peer: $cliqueInfo")
    table += cliqueInfo.id -> PeerStatus.fromInfo(cliqueInfo)
    fetchNeighbors(cliqueInfo)
  }

  def scan(): Unit = {
    val sortedNeighbors =
      AVector.from(table.values).sortBy(status => selfCliqueInfo.id.hammingDist(status.info.id))
    sortedNeighbors
      .takeUpto(config.scanMaxPerGroup)
      .foreach(status => fetchNeighbors(status.info))
    val emptySlotNum = config.scanMaxPerGroup - sortedNeighbors.length
    val bootstrapNum = if (emptySlotNum > 0) emptySlotNum else 0
    bootstrap.takeUpto(bootstrapNum).foreach(tryPing)
  }

  def shouldScanFast(): Boolean = {
    table.isEmpty
  }

  def fetchNeighbors(info: CliqueInfo): Unit = {
    fetchNeighbors(info.masterAddress)
  }

  def fetchNeighbors(remote: InetSocketAddress): Unit = {
    send(remote, FindNode(selfCliqueInfo.id))
  }

  def send(remote: InetSocketAddress, payload: Payload): Unit = {
    val message = DiscoveryMessage.from(selfCliqueInfo.id, payload)
    socket ! Udp.Send(DiscoveryMessage.serialize(message), remote)
  }

  def tryPing(cliqueInfo: CliqueInfo): Unit = {
    if (isUnknown(cliqueInfo.id) && isPendingAvailable) {
      log.info(s"Sending Ping to $cliqueInfo")
      send(cliqueInfo.masterAddress, Ping(selfCliqueInfo)) // TODO: Improve this
      pendings += (cliqueInfo.id -> AwaitPong(cliqueInfo.masterAddress, System.currentTimeMillis()))
    }
  }

  def tryPing(remote: InetSocketAddress): Unit = {
    log.info(s"Sending Ping to $remote")
    send(remote, Ping(selfCliqueInfo))
  }

  def handlePong(cliqueInfo: CliqueInfo): Unit = {
    val cliqueId = cliqueInfo.id
    pendings.get(cliqueId) match {
      case Some(AwaitPong(_, _)) =>
        pendings.remove(cliqueId)
        if (table.size < config.neighborsPerGroup) {
          appendPeer(cliqueInfo)
        } else {
          tryInsert(cliqueInfo)
        }
      case None =>
        tryPing(cliqueInfo)
    }
  }

  def tryInsert(cliqueInfo: CliqueInfo): Unit = {
    val myself   = selfCliqueInfo.id
    val furthest = table.keys.maxBy(myself.hammingDist)
    if (myself.hammingDist(cliqueInfo.id) < myself.hammingDist(furthest)) {
      table -= furthest
      appendPeer(cliqueInfo)
    }
  }
}
