package org.alephium.rpc

import java.net.{InetAddress, InetSocketAddress}

import scala.reflect.ClassTag

import io.circe._

import org.alephium.util.AVector

object CirceUtils {
  implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  def codecXmap[T, U](to: T => U, from: U => T)(implicit codec: Codec[T]): Codec[U] = {
    val encoder = codec.contramap(from)
    val decoder = codec.map(to)
    Codec.from(decoder, encoder)
  }

  def codecXemap[T, U](to: T => Either[String, U], from: U => T)(implicit _encoder: Encoder[T],
                                                                 _decoder: Decoder[T]): Codec[U] = {
    val encoder = _encoder.contramap(from)
    val decoder = _decoder.emap(to)
    Codec.from(decoder, encoder)
  }

  implicit def arrayEncoder[A: ClassTag](implicit encoder: Encoder[A]): Encoder[Array[A]] =
    (as: Array[A]) => Json.fromValues(as.map(encoder.apply))

  implicit def arrayDecoder[A: ClassTag](implicit decoder: Decoder[A]): Decoder[Array[A]] =
    Decoder.decodeArray[A]

  implicit def arrayCodec[A: ClassTag](implicit encoder: Encoder[A],
                                       decoder: Decoder[A]): Codec[Array[A]] = {
    Codec.from(arrayDecoder[A], arrayEncoder[A])
  }

  implicit def avectorEncoder[A: ClassTag](implicit encoder: Encoder[A]): Encoder[AVector[A]] =
    (as: AVector[A]) => Json.fromValues(as.toIterable.map(encoder.apply))

  implicit def avectorDecoder[A: ClassTag](implicit decoder: Decoder[A]): Decoder[AVector[A]] =
    Decoder.decodeArray[A].map(AVector.unsafe)

  implicit def avectorCodec[A: ClassTag](implicit encoder: Encoder[A],
                                         decoder: Decoder[A]): Codec[AVector[A]] = {
    Codec.from(avectorDecoder[A], avectorEncoder[A])
  }

  implicit val inetAddressCodec: Codec[InetAddress] = {
    codecXemap[String, InetAddress](createInetAddress, _.getHostAddress)
  }

  private def createInetAddress(s: String): Either[String, InetAddress] = {
    try Right(InetAddress.getByName(s))
    catch { case e: Throwable => Left(e.getMessage) }
  }

  implicit val socketAddressCodec: Codec[InetSocketAddress] = {
    val encoder = Encoder.forProduct2[InetSocketAddress, InetAddress, Int]("addr", "port")(sAddr =>
      (sAddr.getAddress, sAddr.getPort))
    val decoder = Decoder
      .forProduct2[(InetAddress, Int), InetAddress, Int]("addr", "port")((_, _))
      .emap { case (iAddr, port) => createSocketAddress(iAddr, port) }
    Codec.from(decoder, encoder)
  }

  private def createSocketAddress(address: InetAddress,
                                  port: Int): Either[String, InetSocketAddress] = {
    try Right(new InetSocketAddress(address, port))
    catch { case e: Throwable => Left(e.getMessage) }
  }
}
