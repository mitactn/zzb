package zzb.rest
package marshalling

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit._
import akka.util.Timeout
import akka.actor.ActorRefFactory
//import spray.http._

/**
 * A MarshallingContext serving as a marshalling receptacle, collecting the output of a Marshaller
 * for subsequent postprocessing.
 */
class CollectingMarshallingContext(implicit actorRefFactory: ActorRefFactory = null) extends MarshallingContext {
  private val _entityAndHeaders = new AtomicReference[Option[(RestEntity, Seq[RestHeader])]](None)
  private val _error = new AtomicReference[Option[Throwable]](None)
  //private val _chunkedMessageEnd = new AtomicReference[Option[ChunkedMessageEnd]](None)
  //private val _chunks = new AtomicReference[Seq[MessageChunk]](Vector.empty)
  private val latch = new CountDownLatch(1)

  def entityAndHeaders: Option[(RestEntity, Seq[RestHeader])] = _entityAndHeaders.get
  def entity: Option[RestEntity] = entityAndHeaders.map(_._1)
  def headers: Seq[RestHeader] = entityAndHeaders.toSeq.flatMap(_._2)
  def error: Option[Throwable] = _error.get
  //def chunks: Seq[MessageChunk] = _chunks.get
  //def chunkedMessageEnd: Option[ChunkedMessageEnd] = _chunkedMessageEnd.get

  // we always convert to the first content-type the marshaller can marshal to
  //def tryAccept(contentTypes: Seq[ContentType]) = contentTypes.headOption

  //def rejectMarshalling(supported: Seq[ContentType]): Unit =
  //  handleError(new RuntimeException("Marshaller rejected marshalling, only supports " + supported))

  def marshalTo(entity: RestEntity, headers: RestHeader*): Unit = {
    if (!_entityAndHeaders.compareAndSet(None, Some(entity -> headers))) sys.error("`marshalTo` called more than once")
    latch.countDown()
  }

  def handleError(error: Throwable): Unit = {
    _error.compareAndSet(None, Some(error)) // we only save the very first error
    latch.countDown()
  }

  //  def startChunkedMessage(entity: RestEntity, ack: Option[Any] = None,
  //                          headers: Seq[RestHeader] = Nil)(implicit sender: ActorRef): ActorRef = {
  //    require(actorRefFactory != null, "Chunked responses can only be collected if an ActorRefFactory is provided")
  //    if (!_entityAndHeaders.compareAndSet(None, Some(entity -> headers)))
  //      sys.error("`marshalTo` or `startChunkedMessage` was already called")
  //
  //    val ref = new UnregisteredActorRef(actorRefFactory) {
  //      def handle(message: Any)(implicit sender: ActorRef): Unit =
  //        message match {
  //          case HttpMessagePartWrapper(part, ack) ⇒
  //            part match {
  //              case x: MessageChunk ⇒
  //                @tailrec def updateChunks(current: Seq[MessageChunk]): Unit =
  //                  if (!_chunks.compareAndSet(current, _chunks.get :+ x)) updateChunks(_chunks.get)
  //                updateChunks(_chunks.get)
  //
  //              case x: ChunkedMessageEnd ⇒
  //                if (!_chunkedMessageEnd.compareAndSet(None, Some(x)))
  //                  sys.error("ChunkedMessageEnd received more than once")
  //                latch.countDown()
  //
  //              case x ⇒ throw new IllegalStateException("Received unexpected message part: " + x)
  //            }
  //            ack.foreach(sender.tell(_, this))
  //        }
  //    }
  //    ack.foreach(sender.tell(_, ref))
  //    ref
  //  }

  def awaitResults(implicit timeout: Timeout): Unit =
    latch.await(timeout.duration.toMillis, MILLISECONDS)
}