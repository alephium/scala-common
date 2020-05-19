package org.alephium

import java.net.{InetAddress, InetSocketAddress, UnknownHostException}

import scala.reflect.ClassTag

import akka.util.ByteString

import org.alephium.util.{AVector, TimeStamp, U32, U64}

package object serde {
  import Serde._

  type SerdeResult[T] = Either[SerdeError, T]

  def serdeImpl[T](implicit sd: Serde[T]): Serde[T] = sd

  def serialize[T](input: T)(implicit serializer: Serializer[T]): ByteString =
    serializer.serialize(input)

  def deserialize[T](input: ByteString)(implicit deserializer: Deserializer[T]): SerdeResult[T] =
    deserializer.deserialize(input)

  implicit val byteSerde: Serde[Byte] = ByteSerde

  implicit val intSerde: Serde[Int] = IntSerde

  implicit val longSerde: Serde[Long] = LongSerde

  implicit val u32Serde: Serde[U32] = intSerde.xmap(U32.unsafe, _.value)

  implicit val u64Serde: Serde[U64] = longSerde.xmap(U64.unsafe, _.value)

  implicit val bytestringSerde: Serde[ByteString] = ByteStringSerde

  implicit val stringSerde: Serde[String] =
    ByteStringSerde.xmap(_.utf8String, ByteString.fromString)

  implicit def optionSerde[T](implicit serde: Serde[T]): Serde[Option[T]] =
    new OptionSerde[T](serde)

  implicit def eitherSerde[A, B](implicit serdeA: Serde[A], serdeB: Serde[B]): Serde[Either[A, B]] =
    new EitherSerde[A, B](serdeA, serdeB)

  def fixedSizeSerde[T: ClassTag](size: Int)(implicit serde: Serde[T]): Serde[AVector[T]] =
    Serde.fixedSizeSerde[T](size, serde)

  implicit def avectorSerde[T: ClassTag](implicit serde: Serde[T]): Serde[AVector[T]] =
    dynamicSizeSerde(serde)

  implicit def avectorSerializer[T: ClassTag](
      implicit serializer: Serializer[T]): Serializer[AVector[T]] =
    new AVectorSerializer[T](serializer)

  implicit def avectorDeserializer[T: ClassTag](
      implicit deserializer: Deserializer[T]): Deserializer[AVector[T]] =
    new AVectorDeserializer[T](deserializer)

  implicit val bigIntSerde: Serde[BigInt] =
    avectorSerde[Byte].xmap(vc => BigInt(vc.toArray), bi => AVector.unsafe(bi.toByteArray))

  /*
   * Note: only ipv4 and ipv6 addresses are supported in the following serdes
   * addresses based on hostnames are not supported
   */

  implicit val inetAddressSerde: Serde[InetAddress] = bytestringSerde
    .xfmap(bs => createInetAddress(bs), ia => ByteString.fromArrayUnsafe(ia.getAddress))

  def createInetAddress(bs: ByteString): SerdeResult[InetAddress] = {
    try Right(InetAddress.getByAddress(bs.toArray))
    catch { case e: UnknownHostException => Left(SerdeError.wrongFormat(e.getMessage)) }
  }

  implicit val inetSocketAddressSerde: Serde[InetSocketAddress] =
    tuple2[InetAddress, Int].xfmap({ case (address, port) => createSocketAddress(address, port) },
                                   sAddress => (sAddress.getAddress, sAddress.getPort))

  def createSocketAddress(inetAddress: InetAddress, port: Int): SerdeResult[InetSocketAddress] = {
    try Right(new InetSocketAddress(inetAddress, port))
    catch { case e: IllegalArgumentException => Left(SerdeError.wrongFormat(e.getMessage)) }
  }

  implicit val serdeTS: Serde[TimeStamp] = longSerde.xomap(TimeStamp.from, _.millis)
}
