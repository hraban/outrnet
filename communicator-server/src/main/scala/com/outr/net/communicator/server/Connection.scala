package com.outr.net.communicator.server

import org.powerscala.event.Listenable
import org.powerscala.event.processor.UnitProcessor
import org.powerscala.log.Logging
import org.powerscala.concurrent.AtomicInt

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Connection(val id: String) extends Listenable with Logging {
  private val lastReceiveId = new AtomicInt(0)
  private val lastSentId = new AtomicInt(0)
  private var lastHeard = System.currentTimeMillis()

  val received = new UnitProcessor[Message]("received")
  val sent = new UnitProcessor[Message]("sent")
  val queued = new UnitProcessor[Message]("queued")

  private var priorityQueue = List.empty[Message]
  private var queue = List.empty[Message]
  private var sentMessages = List.empty[Message]

  /**
   * Send a message to the client.
   *
   * @param event the message event type
   * @param data the data of the message
   * @param highPriority if set to true the message will appear before all standard messages in the queue (default: false)
   */
  def send(event: String, data: Any = null, highPriority: Boolean = false) = synchronized {
    val sendId = if (highPriority) -1 else lastSentId.addAndGet(1)
    val message = Message(sendId, event, data)
    if (highPriority) {
      priorityQueue = message :: queue
    } else {
      queue = message :: queue
    }
    queued.fire(message)
  }

  def hasMessage = priorityQueue.nonEmpty || queue.nonEmpty

  def messages(lastSentId: Int) = synchronized {
    if (priorityQueue.nonEmpty || queue.nonEmpty) {
      if (queue.nonEmpty && queue.head.id != lastSentId + 1) {
        throw new MessageException(s"Last Sent ID is not correct. Expected: ${lastSentId + 1}, Received: ${queue.head.id}.", MessageReceiveFailure.InvalidMessageId)
      }
      val messages = priorityQueue.reverse ::: queue.reverse
      sentMessages = messages
      priorityQueue = List.empty
      queue = List.empty
      messages
    } else {
      Nil
    }
  }

  /**
   * Receives a message from the client.
   *
   * @param message the message to receive
   * @throws MessageException if the message id is not in the correct order
   */
  def receive(message: Message) = synchronized {
    heardFrom()             // Update the lastHeardFrom upon receipt of any message from client
    val expectedId = lastReceiveId.get() + 1
    if (message.id != -1 && message.id != expectedId) {
      val text = s"Invalid message id received on server: ${message.id}, but expected: $expectedId"
      throw new MessageException(text, MessageReceiveFailure.InvalidMessageId)
    }
    received.fire(message)
    if (message.id != -1) {
      lastReceiveId.set(expectedId)
    }
  }

  def update(time: Long) = {
    val delta = (time - lastHeardFrom) / 1000.0
    if (delta > Communicator.connectionTimeout()) {
      Communicator.dispose(this)
    }
  }

  def dispose() = {
  }

  def lastHeardFrom = lastHeard
  def heardFrom() = lastHeard = System.currentTimeMillis()
}
