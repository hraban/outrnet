package com.outr.net.examples

import com.outr.net.http.WebApplication
import com.outr.net.communicator.server.{PongResponder, Communicator}
import org.powerscala.log.Logging
import com.outr.net.http.session.MapSession
import com.outr.net.http.request.HttpRequest
import com.outr.net.http.handler.CachedHandler
import com.outr.net.http.jetty.JettyApplication

/**
 * @author Matt Hicks <matt@outr.com>
 */
object ExampleWebApplication extends WebApplication[MapSession] with Logging with JettyApplication {
  PongResponder.connect()     // Support ping-pong
  TimeResponder.connect()     // Support time request

  protected def createSession(request: HttpRequest, id: String) = new MapSession(id)

  def init() = {
    handlers += CachedHandler     // Add caching support
    Communicator.configure(this)

    // Add example html files
    addClassPath("/", "html/")

    Communicator.created.on {
      case (connection, data) => {
        info(s"Created! ${connection.id} - $data")
        connection.received.on {
          case message => info(s"Message Received: $message")
        }
      }
    }
    Communicator.connected.on {
      case (connection, data) => {
        info(s"Connected! ${connection.id} - $data")
      }
    }
    Communicator.disconnected.on {
      case connection => {
        info(s"Disconnected! ${connection.id}")
      }
    }
    Communicator.disposed.on {
      case connection => {
        info(s"Disposed! ${connection.id}")
      }
    }
  }

  override def dispose() = {
    super.dispose()
    info("Disposed application!")
  }
}
