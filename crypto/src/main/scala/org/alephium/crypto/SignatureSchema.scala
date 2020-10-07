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

package org.alephium.crypto

import akka.util.ByteString

import org.alephium.serde.RandomBytes
import org.alephium.util.AVector

trait PrivateKey extends RandomBytes

trait PublicKey extends RandomBytes

trait Signature extends RandomBytes

trait SignatureSchema[D <: PrivateKey, Q <: PublicKey, S <: Signature] {

  def generatePriPub(): (D, Q)

  def sign(message: ByteString, privateKey: D): S

  def sign(message: AVector[Byte], privateKey: D): S

  def verify(message: ByteString, signature: S, publicKey: Q): Boolean

  def verify(message: AVector[Byte], signature: S, publicKey: Q): Boolean
}
