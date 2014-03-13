//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

/** A container for a single optional value, which may be observed for changes. */
class OptValue[T] private (init :T) extends OptValueV[T] {

  /** Updates this instance with the supplied value. Registered listeners are notified only if the
    * value differs from the current value.
    *
    * @throws $EXNDOC
    */
  def update (value :T) {
    if (value == null) throw new IllegalArgumentException("Null values not allowed")
    updateAndNotifyIf(value)
  }

  /** Updates this instance with the supplied value. Registered listeners are notified regardless of
    * whether the new value is equal to the old value.
    *
    * @throws $EXNDOC
    */
  def updateForce (value :T) {
    if (value == null) throw new IllegalArgumentException("Null values not allowed")
    updateAndNotify(value)
  }

  /** Clears the value in this instance, making it empty. Registered listeners are notified only if
    * the value was not previously empty.
    *
    * @throws $EXNDOC
    */
  def clear () :Unit = updateAndNotifyIf(null.asInstanceOf[T])

  /** Clears the value in this instance, making it empty. Registered listeners are notified
    * regardless of whether the value was previously empty.
    *
    * @throws $EXNDOC
    */
  def clearForce () :Unit = updateAndNotify(null.asInstanceOf[T])

  override def get :T = if (_value == null) throw new NoSuchElementException() else _value
  override def isDefined = _value != null
  override protected def updateLocal (value :T) = {
     val oldValue = _value
     _value = value
    oldValue
  }

  private[this] var _value :T = init
}

/** Helper methods for values. */
object OptValue {

  /** Creates an instance with the specified starting value. */
  def apply[T] (init :T) = new OptValue[T](init)

  /** Creates an instance with an empty starting value. */
  def apply[T] () = new OptValue[T](null.asInstanceOf[T])
}
