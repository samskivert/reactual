//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

/** A view of a [[Value]] which may be observed, but not updated.
  */
abstract class ValueV[T] extends Reactor[(T,T) => Unit] with PropertyV[T] {

  /** Returns the current value. */
  def get :T

  /** Returns the current value. This is a synonym for [[get]] so that one can use Scala's special
    * apply syntax (e.g. `myval()` instead of `myval.get`). */
  def apply () :T = get

  /** Maps the output of this value via `f`. When this value is updated, the mapped value will emit
    * that value as transformed by `f`. A call to `get` on the mapped value will call get on this
    * value and transform the result via `f` before returning it. The mapped value will retain a
    * connection to this value for as long as it has connections of its own.
    */
  def map[M] (f :T => M) :ValueV[M] = {
    val outer = this
    new ValueV[M]() {
      override def get = f(outer.get)
      // connectionAdded and connectionRemoved are only ever called with a lock held on this reactor,
      // so we're safe in checking and mutating _conn
      override protected def connectionAdded () {
        super.connectionAdded()
        if (_conn == null) _conn = outer.onChange((v,o) => notifyEmit(f(v), f(o)))
      }
      override protected def connectionRemoved () {
        super.connectionRemoved()
        if (!hasConnections && _conn != null) {
          _conn.close()
          _conn = null
        }
      }
      protected var _conn :Connection = _
    }
  }

  /** Connects the supplied "value agnostic" block of code with priority 0. When a value is emitted,
    * the block will be executed. Useful when you don't care about the value.
    * @return $CONDOC
    */
  def onEmit (block : =>Unit) :Connection = addConnection(0, (_,_) => block)

  /** Connects the supplied "value agnostic" block of code at the specified priority. When a value is
    * emitted, the block will be executed. Useful when you don't care about the value.
    * @param prio $PRIODOC
    * @return $CONDOC
    */
  def onEmitAt (prio :Int)(block : =>Unit) :Connection = addConnection(prio, (_,_) => block)

  /** Connects the supplied slot (side-effecting function) with priorty zero. When a value is
    * emitted, the slot will be invoked with the value.
    * @return $CONDOC
    */
  def onValue (slot :T => Unit) :Connection = onValueAt(0)(slot)

  /** Connects the supplied slot (side-effecting function) at the specified priority. When a value is
    * emitted, the slot will be invoked with the value.
    * @param prio $PRIODOC
    * @return $CONDOC
    */
  def onValueAt (prio :Int)(slot :T =>Unit) :Connection = addConnection(prio, (c, _) => slot(c))

  /** Connects `slot` to this value with priority 0; it will be invoked when the value changes. Also
    * immediately invokes `slot` with the current value.
    * @return $CONDOC
    */
  def onValueNotify (slot :T => Unit) :Connection = onValueNotifyAt(0)(slot)

  /** Connects `slot` to this value; it will be invoked when the value changes. Also immediately
    * invokes `slot` with the current value.
    * @param prio $PRIODOC
    * @return $CONDOC
    */
  def onValueNotifyAt (prio :Int)(slot :T => Unit) :Connection = {
    // connect before notifying the slot; if the slot changes the value during execution, it will
    // expect to be notified of that change; but if the slot throws an exception, we need to take
    // care of disconnecting because the returned connection will never reach the caller
    val conn = onValueAt(prio)(slot)
    try {
      slot(get)
      conn
    } catch {
      case e :Throwable => conn.close(); throw e
    }
  }

  /** Connects the supplied slot (side-effecting function) with priorty zero. When a value is
    * emitted, the slot will be invoked with the value.
    * @return $CONDOC
    */
  def onChange (slot :(T,T) => Unit) :Connection = onChangeAt(0)(slot)

  /** Connects the supplied slot (side-effecting function) at the specified priority. When a value is
    * emitted, the slot will be invoked with the value.
    * @param prio $PRIODOC
    * @return $CONDOC
    */
  def onChangeAt (prio :Int)(slot :(T,T)=>Unit) :Connection = addConnection(prio, slot)

  override def hashCode = get match {
    case null => 0
    case v => v.hashCode
  }

  override def equals (other :Any) = {
    if (other == null) false
    else if (other.getClass != getClass) false
    else get == other.asInstanceOf[ValueV[_]].get
  }

  override def toString :String = s"$shortClassName($get)"

  /** Updates the value contained in this instance and notifies registered listeners iff said value
    * is not equal to the value already contained in this instance.
    */
  protected def updateAndNotifyIf (value :T) :T = updateAndNotify(value, false)

  /** Updates the value contained in this instance and notifies registered listeners.
    * @return the previously contained value.
    */
  protected def updateAndNotify (value :T) :T = updateAndNotify(value, true)

  /** Updates the value contained in this instance and notifies registered listeners.
    * @param force if true, the listeners will always be notified, if false the will be notified
    * only if the new value is not equal to the old value.
    * @return the previously contained value.
    */
  protected def updateAndNotify (value :T, force :Boolean) : T = {
    checkMutate()
    val ovalue = updateLocal(value)
    if (force || value != ovalue) emitChange(value, ovalue)
    ovalue
  }

  /** Emits a changed value. Default implementation immediately notifies listeners. */
  protected def emitChange (value :T, ovalue :T) = notifyEmit(value, ovalue)

  /** Updates our locally stored value. Default implementation throws unsupported operation.
    * @return the previously stored value.
    */
  protected def updateLocal (value :T) :T = throw new UnsupportedOperationException

  /** Emits the supplied value to all connections. */
  protected def notifyEmit (value :T, ovalue :T) {
    val lners = prepareNotify()
    var err :ReactionException = null
    try {
      var cons = lners
      while (cons != null) {
        try {
          cons.listener.apply(value, ovalue)
        } catch {
          case t :Throwable =>
            if (err == null) err = new ReactionException()
            err addSuppressed t
        }
        if (cons.oneShot) cons.close()
        cons = cons.next
      }
    } finally {
      finishNotify(lners)
    }
    if (err != null) throw err
  }
}
