package com.outr.net

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait IP {
  def address: Array[Int]
  def addressString: String

  override def toString = addressString
}

object IP {
  lazy val LocalHost = IPv4()

  val IPv4Regex = """\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b""".r
  val IPv6Regex = """(\d*):(\d*):(\d*):(\d*):(\d*):(\d*):(\d*):(\d*)""".r

  def get(address: String) = address match {
    case IPv4Regex(p1, p2, p3, p4) => Some(IPv4(p1.toInt, p2.toInt, p3.toInt, p4.toInt))
    case IPv6Regex(p1, p2, p3, p4, p5, p6, p7, p8) => Some(IPv6(p1.toInt, p2.toInt, p3.toInt, p4.toInt, p5.toInt, p6.toInt, p7.toInt, p8.toInt))
    case _ => None
  }

  def apply(address: String) = get(address).getOrElse(throw new NullPointerException(s"Unable to parse: $address to IP address."))
}