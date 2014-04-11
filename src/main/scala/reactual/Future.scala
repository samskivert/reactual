//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

import scala.collection.mutable.ArrayBuffer
import scala.util.{Try, Success, Failure}

/** Represents an asynchronous result. Unlike Java or Scala futures, you cannot block on this
  * result. You can [[map]] or [[flatMap]] it, and listen for success or failure via the
  * [[Future.success]] and [[Future.failure]] signals.
  */
class Future[+T] protected (_result :ValueV[Try[T]]) {

  /** Causes `slot` to be notified if/when this future is completed with success. If it has already
    * suceeded, the slot will be notified immediately.
    * @return this future for chaining.
    */
  def onSuccess (slot :T => Unit) :this.type = _result.get match {
    case null => _result.onValue(_.foreach(slot)) ; this
    case r    => r.foreach(slot) ; this
  }

  /** Causes `slot` to be notified if/when this future is completed with failure. If it has already
    * failed, the slot will be notified immediately.
    * @return this future for chaining.
    */
  def onFailure (slot :Throwable => Unit) :this.type = _result.get match {
    case null => _result.onValue(foreachFailure(_, slot)) ; this
    case r    => foreachFailure(r, slot) ; this
  }

  /** Causes `success` to be notified if/when this future is completed with success, and `failure` to
    * be notified if/when this future is completed with failure. If the future has already
    * completed, the appropriate function will be called immediately.
    * @return this future for chaining.
    */
  def onComplete (success :T => Unit, failure :Throwable => Unit) :this.type =
    onSuccess(success).onFailure(failure)

  /** Causes `slot` to be notified when this future is completed. If it has already completed, the
    * slot will be notified immediately.
    * @return this future for chaining.
    */
  def onComplete (slot :Try[T] => Unit) :this.type = _result.get match {
    case null => _result.onValue(slot) ; this
    case r    => slot(r) ; this
  }

  /** Returns a value that indicates whether this future has completed. */
  def isComplete :ValueV[Boolean] = _result.map(_ != null)

  /** Convenience method to bind `slot` to [[isComplete]] via [[ValueV.onValueNotify]]. This is
    * useful for binding the disabled state of UI elements to this future's completeness (i.e.
    * disabled while the future is incomplete, then reenabled when it is completed).
    * @return this future for chaining.
    */
  def bindComplete (slot :Boolean => Unit) :this.type = { isComplete.onValueNotify(slot) ; this }

  /** Maps the value of a successful result using `func` upon arrival. */
  def map[R] (func :T => R) :Future[R] = new Future[R](_result.map(_ match {
    case null => null
    case r    => r.map(func)
  }))

  /** Maps a successful result to a new result using `func` when it arrives. Failure on the original
    * result or the mapped result are both dispatched to the mapped result. This is useful for
    * chaining asynchronous actions. It's also known as monadic bind.
    */
  def flatMap[R] (func :T => Future[R]) :Future[R] = {
    val mapped = Value[Try[R]](null)
    _result.onValueNotify { _ match {
      case null => () // nada; source future not yet complete nothing to do
      case Failure(e) => mapped.update(Failure[R](e))
      case Success(v) => func(v).onComplete(mapped.update _)
    }}
    new Future[R](mapped)
  }

  private def foreachFailure (t :Try[_], slot :Throwable => Unit) = t match {
    case Failure(e) => slot(e)
    case _ => // nada
  }
}

object Future {

  /** Creates a future with a pre-existing success value. */
  def success[T] (value :T) :Future[T] = result(Success(value))

  /** Creates a future with a pre-existing failure value. */
  def failure[T] (cause :Throwable) :Future[T] = result(Failure(cause))

  /** Creates a future with an already-computed result. */
  def result[T] (result :Try[T]) :Future[T] = new Future(Value(result))

  /** Returns a future containing a list of all success results from `futures` if all of the futures
    * complete successfully, or a [[ReactionException]] aggregating all failures, if any of the
    * futures fails.
    *
    * If `futures` is an ordered collection, the resulting list will match the order of the
    * futures. If not, the result list is in `futures`' iteration order.
    */
  def sequence[T] (futures :Seq[Future[T]]) :Future[Seq[T]] = {
    // if we're passed an empty list of futures, "succeed" immediately; if we followed the other
    // branch we'd return a future that never succeeded nor failed which is not useful
    if (futures.isEmpty) success(Seq())
    else {
      val pseq = Promise[Seq[T]]()
      class Sequencer {
        def onResult (idx :Int, result :Try[T]) = synchronized {
          result match {
            case Success(v) => _results(idx) = v
            case Failure(e) =>
              if (_err == null) _err = new ReactionException()
              _err addSuppressed e
          }
          _remain -= 1
          if (_remain == 0) {
            if (_err != null) pseq.fail(_err)
            // we know that Array[Any] can safely be turned into Seq[T], so we cheat
            else pseq.succeed(_results.asInstanceOf[Array[T]])
          }
        }
        private[this] val _results = Array.ofDim[Any](futures.size)
        private[this] var _remain :Int = futures.size
        private[this] var _err :ReactionException = _
      }
      val seq = new Sequencer()
      var ii = 0
      val iter = futures.iterator
      while (iter.hasNext) {
        val idx = ii
        iter.next().onComplete(result => seq.onResult(idx, result))
        ii += 1
      }
      pseq
    }
  }

  /** Returns a future containing a list of all success results from `futures`. Any failure results
    * are simply omitted from the list. The success results are also in no particular order. If all
    * of `futures` fail, the resulting list will be empty.
    */
  def collect[T] (futures :Seq[Future[T]]) :Future[Seq[T]] = {
    // if we're passed an empty list of futures, "succeed" immediately; if we followed the other
    // branch we'd return a future that never succeeded nor failed which is not useful
    if (futures.isEmpty) success(Seq())
    else {
      val pseq = Promise[Seq[T]]()
      val collector = new Function1[Try[T],Unit]() {
        def apply (result :Try[T]) = synchronized {
          if (result.isSuccess) _results += result.get
          _remain -= 1
          if (_remain == 0) pseq.succeed(_results)
        }
        private[this] val _results = new ArrayBuffer[T]()
        private[this] var _remain = futures.size
      }
      futures.foreach(_.onComplete(collector))
      pseq
    }
  }
}
