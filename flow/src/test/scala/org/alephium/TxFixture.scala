package org.alephium

import io.circe.parser.parse
import org.alephium.crypto.{ED25519PrivateKey, ED25519PublicKey}
import org.alephium.flow.constant.{Consensus, Genesis}
import org.alephium.protocol.model._
import org.alephium.util.{AVector, Hex}

import scala.io.Source

trait TxFixture {

  def blockForTransfer(to: ED25519PublicKey, value: BigInt): Block = {
    assert(value >= 0)

    val txOutput1 = TxOutput(value, to)
    val txOutput2 = TxOutput(testBalance - value, testPublicKey)
    val txInput   = TxInput(Genesis.block.transactions.head.hash, 0)
    val transaction = Transaction.from(
      UnsignedTransaction(AVector(txInput), AVector(txOutput1, txOutput2)),
      testPrivateKey
    )
    Block.from(AVector(Genesis.block.hash), AVector(transaction), Consensus.maxMiningTarget, 0)
  }

  private val json = parse(Source.fromResource("genesis.json").mkString).right.get

  private val test = json.hcursor.downField("test")
  val testPrivateKey: ED25519PrivateKey =
    ED25519PrivateKey.unsafeFrom(Hex.unsafeFrom(test.get[String]("privateKey").right.get))
  val testPublicKey: ED25519PublicKey =
    ED25519PublicKey.unsafeFrom(Hex.unsafeFrom(test.get[String]("publicKey").right.get))
  val testBalance: BigInt = test.get[BigInt]("balance").right.get
}
