package org.alephium.protocol.message

import akka.util.ByteString
import org.alephium.crypto.Keccak256
import org.alephium.protocol.Protocol
import org.alephium.protocol.config.GroupConfig
import org.alephium.protocol.model.{Block, BlockHeader, CliqueInfo}
import org.alephium.serde._
import org.alephium.util.AVector

sealed trait Payload

object Payload {
  def serialize(payload: Payload): ByteString = {
    val (code, data) = payload match {
      case x: Hello       => (Hello, Serializer[Hello].serialize(x))
      case x: HelloAck    => (HelloAck, Serializer[HelloAck].serialize(x))
      case x: Ping        => (Ping, Serializer[Ping].serialize(x))
      case x: Pong        => (Pong, Serializer[Pong].serialize(x))
      case x: SendBlocks  => (SendBlocks, Serializer[SendBlocks].serialize(x))
      case x: GetBlocks   => (GetBlocks, Serializer[GetBlocks].serialize(x))
      case x: SendHeaders => (SendHeaders, Serializer[SendHeaders].serialize(x))
      case x: GetHeaders  => (GetHeaders, Serializer[GetHeaders].serialize(x))
    }
    Serde[Int].serialize(Code.toInt(code)) ++ data
  }

  val deserializerCode: Deserializer[Code] =
    Serde[Int].validateGet(Code.fromInt, c => s"Invalid code $c")

  def _deserialize(input: ByteString)(
      implicit config: GroupConfig): SerdeResult[(Payload, ByteString)] = {
    deserializerCode._deserialize(input).flatMap {
      case (code, rest) =>
        code match {
          case Hello       => _deserializeHandShake(new Hello(_, _, _, _))(rest)
          case HelloAck    => _deserializeHandShake(new HelloAck(_, _, _, _))(rest)
          case Ping        => Serde[Ping]._deserialize(rest)
          case Pong        => Serde[Pong]._deserialize(rest)
          case SendBlocks  => Serde[SendBlocks]._deserialize(rest)
          case GetBlocks   => Serde[GetBlocks]._deserialize(rest)
          case SendHeaders => Serde[SendHeaders]._deserialize(rest)
          case GetHeaders  => Serde[GetHeaders]._deserialize(rest)
        }
    }
  }

  def _deserializeHandShake[T <: Payload](build: (Int, Long, CliqueInfo, Int) => T)(
      input: ByteString)(implicit config: GroupConfig): SerdeResult[(Payload, ByteString)] = {
    HandShake.Unsafe.serde._deserialize(input).flatMap[SerdeError, (Payload, ByteString)] {
      case (unsafe, rest) =>
        unsafe.validate(build) match {
          case Left(error)  => Left(SerdeError.validation(error))
          case Right(hello) => Right((hello, rest))
        }
    }
  }

  sealed trait Code
  object Code {
    private val values: AVector[Code] =
      AVector(Hello, HelloAck, Ping, Pong, SendBlocks, GetBlocks, SendHeaders, GetHeaders)

    val toInt: Map[Code, Int] = values.toIterable.zipWithIndex.toMap
    def fromInt(code: Int): Option[Code] =
      if (code >= 0 && code < values.length) Some(values(code)) else None
  }
}

sealed trait HandShake extends Payload

object HandShake {
  class Unsafe(val version: Int,
               val timestamp: Long,
               val cliqueInfoUnsafe: CliqueInfo.Unsafe,
               val brokerIndex: Int) {
    def validate[T](build: (Int, Long, CliqueInfo, Int) => T)(
        implicit config: GroupConfig): Either[String, T] = {
      if (version != Protocol.version) {
        Left(s"Invalid protoco version: got $version, expect ${Protocol.version}")
      } else if (timestamp <= 0) {
        Left(s"Invalid timestamp: got $timestamp, expect positive number")
      } else {
        cliqueInfoUnsafe.validate match {
          case Left(message) => Left(message)
          case Right(cliqueInfo) =>
            if (brokerIndex < 0 || brokerIndex >= cliqueInfo.brokerNum) {
              Left(
                s"Invalid brokerIndex: got $brokerIndex, should >= 0 and < ${cliqueInfo.brokerNum}")
            } else Right(build(version, timestamp, cliqueInfo, brokerIndex))
        }
      }
    }
  }
  object Unsafe {
    implicit val serde: Serde[Unsafe] = Serde.forProduct4(
      new Unsafe(_, _, _, _),
      t => (t.version, t.timestamp, t.cliqueInfoUnsafe, t.brokerIndex))
  }
}

class Hello(val version: Int, val timestamp: Long, val cliqueInfo: CliqueInfo, val brokerId: Int)
    extends HandShake

object Hello extends Payload.Code {
  implicit val serializer: Serializer[Hello] =
    Serializer.forProduct4(t => (t.version, t.timestamp, t.cliqueInfo, t.brokerId))

  def apply(cliqueInfo: CliqueInfo, brokerId: Int): Hello = {
    new Hello(Protocol.version, System.currentTimeMillis(), cliqueInfo, brokerId)
  }
}

class HelloAck(val version: Int, val timestamp: Long, val cliqueInfo: CliqueInfo, val brokerId: Int)
    extends HandShake

object HelloAck extends Payload.Code {
  implicit val serializer: Serializer[HelloAck] =
    Serializer.forProduct4(t => (t.version, t.timestamp, t.cliqueInfo, t.brokerId))

  def apply(cliqueInfo: CliqueInfo, brokerId: Int): HelloAck = {
    new HelloAck(Protocol.version, System.currentTimeMillis(), cliqueInfo, brokerId)
  }
}

case class Ping(nonce: Int, timestamp: Long) extends Payload

object Ping extends Payload.Code {
  implicit val serde: Serde[Ping] = Serde.forProduct2(apply, p => (p.nonce, p.timestamp))
}

case class Pong(nonce: Int) extends Payload

object Pong extends Payload.Code {
  implicit val serde: Serde[Pong] = Serde.forProduct1(apply, p => p.nonce)
}

case class SendBlocks(blocks: AVector[Block]) extends Payload

object SendBlocks extends Payload.Code {
  implicit val serde: Serde[SendBlocks] = Serde.forProduct1(apply, p => p.blocks)
}

case class GetBlocks(locators: AVector[Keccak256]) extends Payload

object GetBlocks extends Payload.Code {
  implicit val serde: Serde[GetBlocks] = Serde.forProduct1(apply, p => p.locators)
}

case class SendHeaders(headers: AVector[BlockHeader]) extends Payload

object SendHeaders extends Payload.Code {
  implicit val serde: Serde[SendHeaders] = Serde.forProduct1(apply, p => p.headers)
}

case class GetHeaders(locators: AVector[Keccak256]) extends Payload

object GetHeaders extends Payload.Code {
  implicit val serde: Serde[GetHeaders] = Serde.forProduct1(apply, p => p.locators)
}
