package org.alephium.protocol.model

import akka.util.ByteString
import org.alephium.crypto.ED25519PublicKey
import org.alephium.macros.HPC.cfor
import org.alephium.protocol.config.GroupConfig
import org.alephium.serde.RandomBytes

import scala.annotation.tailrec

/** 160bits identifier of a Peer **/
class PeerId private (val bytes: ByteString) extends RandomBytes {
  def groupIndex(implicit config: GroupConfig): GroupIndex = {
    GroupIndex((bytes.last & 0xFF) % config.groups)
  }

  def hammingDist(another: PeerId): Int = {
    PeerId.hammingDist(this, another)
  }
}

object PeerId extends RandomBytes.Companion[PeerId](new PeerId(_), _.bytes) {
  override def length: Int = peerIdLength

  def fromPublicKey(key: ED25519PublicKey): PeerId = new PeerId(key.bytes)

  def hammingDist(peerId0: PeerId, peerId1: PeerId): Int = {
    val bytes0 = peerId0.bytes
    val bytes1 = peerId1.bytes
    var dist   = 0
    cfor(0)(_ < length, _ + 1) { i =>
      dist += hammingDist(bytes0(i), bytes1(i))
    }
    dist
  }

  def hammingOrder(target: PeerId): Ordering[PeerId] = Ordering.by(hammingDist(target, _))

  private val countLookUp = Array(0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4)

  def hammingDist(byte0: Byte, byte1: Byte): Int = {
    val xor = byte0 ^ byte1
    countLookUp(xor & 0x0F) + countLookUp((xor >> 4) & 0x0F)
  }

  def generateFor(mainGroup: GroupIndex)(implicit config: GroupConfig): PeerId = {
    assert(mainGroup.value < config.groups)

    @tailrec
    def iter(): PeerId = {
      val id = PeerId.generate
      if (id.groupIndex == mainGroup) {
        id
      } else iter()
    }
    iter()
  }
}
