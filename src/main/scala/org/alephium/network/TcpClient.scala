package org.alephium.network

import java.net.InetSocketAddress

import akka.actor.Props
import akka.io.{IO, Tcp}

object TcpClient {
  def props(remote: InetSocketAddress): Props = Props(new TcpClient(remote))
}

class TcpClient(remote: InetSocketAddress) extends TcpHandler {

  import context.system

  IO(Tcp) ! Tcp.Connect(remote)

  override def remoteAddress: InetSocketAddress = remote

  override def receive: Receive = connecting

  def connecting: Receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      logger.debug("Connect failed")
    case Tcp.Connected(remoteAddress, localAddress) =>
      require(remoteAddress == remote)
      logger.debug(s"Connect to $remoteAddress, Listen at $localAddress")
      context.parent ! "Connected"
      val connection = sender()
      connection ! Tcp.Register(self)
      context.become(handle(connection))
  }
}