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

import java.net.InetSocketAddress

class NetworkSpec extends AlephiumSpec {

  it should "parse zero address" in {
    val input     = ""
    val addresses = Network.parseAddresses(input)
    addresses.length is 0
  }

  it should "parse single address" in {
    val input     = "127.0.0.1:1000"
    val addresses = Network.parseAddresses(input)
    addresses.length is 1
    addresses(0) is new InetSocketAddress("127.0.0.1", 1000)
  }

  it should "parse several addresses" in {
    val input     = "127.0.0.1:1000;127.0.0.1:1001;127.0.0.1:1002;127.0.0.1:1003"
    val addresses = Network.parseAddresses(input)
    addresses.length is 4
    addresses(0) is new InetSocketAddress("127.0.0.1", 1000)
    addresses(1) is new InetSocketAddress("127.0.0.1", 1001)
    addresses(2) is new InetSocketAddress("127.0.0.1", 1002)
    addresses(3) is new InetSocketAddress("127.0.0.1", 1003)
  }
}
