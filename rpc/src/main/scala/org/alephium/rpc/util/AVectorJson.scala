package org.alephium.rpc.util

import scala.reflect.ClassTag

import io.circe._

import org.alephium.util.AVector

object AVectorJson {
  def decodeAVector[A: ClassTag](implicit A: Decoder[Array[A]]): Decoder[AVector[A]] =
    A.map(AVector.unsafe)

  def encodeAVector[A](implicit encoder: Encoder[A]): Encoder[AVector[A]] =
    new Encoder[AVector[A]] {
      final def apply(as: AVector[A]): Json = {
        Json.fromValues(as.map(encoder.apply).toIterable)
      }
    }
}
