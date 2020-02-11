package org.alephium.serde

sealed abstract class SerdeError(message: String) extends Exception(message)

object SerdeError {
  class NotEnoughBytes(message: String) extends SerdeError(message)
  class WrongFormat(message: String)    extends SerdeError(message)
  class Validation(message: String)     extends SerdeError(message)
  class Other(message: String)          extends SerdeError(message)

  def notEnoughBytes(expected: Int, got: Int): NotEnoughBytes =
    new NotEnoughBytes(s"Too few bytes: expected $expected, got $got")

  def redundant(expected: Int, got: Int): WrongFormat =
    new WrongFormat(s"Too many bytes: expected $expected, got $got")

  def validation(message: String): Validation = new Validation(message)

  def wrongFormat(message: String): WrongFormat = new WrongFormat(message)

  def other(message: String): Other = new Other(message)
}
