package org.alephium.util

import akka.util.ByteString

object Bits {
  def toPosInt(byte: Byte): Int = {
    byte & 0xFF
  }

  def toBytes(value: Int): ByteString = {
    ByteString((value >> 24).toByte, (value >> 16).toByte, (value >> 8).toByte, value.toByte)
  }

  def xorByte(value: Int): Byte = {
    val byte0 = (value >> 24).toByte
    val byte1 = (value >> 16).toByte
    val byte2 = (value >> 8).toByte
    val byte3 = value.toByte
    (byte0 ^ byte1 ^ byte2 ^ byte3).toByte
  }
}
