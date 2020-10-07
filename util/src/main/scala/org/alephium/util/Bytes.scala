// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

package org.alephium.util

import akka.util.ByteString

object Bytes {
  def toPosInt(byte: Byte): Int = {
    byte & 0xFF
  }

  def toBytes(value: Int): ByteString = {
    ByteString((value >> 24).toByte, (value >> 16).toByte, (value >> 8).toByte, value.toByte)
  }

  def toIntUnsafe(bytes: ByteString): Int = {
    assume(bytes.length == 4)
    bytes(0) << 24 | (bytes(1) & 0xFF) << 16 | (bytes(2) & 0xFF) << 8 | (bytes(3) & 0xFF)
  }

  def toBytes(value: Long): ByteString = {
    ByteString((value >> 56).toByte,
               (value >> 48).toByte,
               (value >> 40).toByte,
               (value >> 32).toByte,
               (value >> 24).toByte,
               (value >> 16).toByte,
               (value >> 8).toByte,
               value.toByte)
  }

  def toLongUnsafe(bytes: ByteString): Long = {
    assume(bytes.length == 8)
    (bytes(0) & 0xFFL) << 56 |
      (bytes(1) & 0xFFL) << 48 |
      (bytes(2) & 0xFFL) << 40 |
      (bytes(3) & 0xFFL) << 32 |
      (bytes(4) & 0xFFL) << 24 |
      (bytes(5) & 0xFFL) << 16 |
      (bytes(6) & 0xFFL) << 8 |
      (bytes(7) & 0xFFL)
  }

  def xorByte(value: Int): Byte = {
    val byte0 = (value >> 24).toByte
    val byte1 = (value >> 16).toByte
    val byte2 = (value >> 8).toByte
    val byte3 = value.toByte
    (byte0 ^ byte1 ^ byte2 ^ byte3).toByte
  }
}
