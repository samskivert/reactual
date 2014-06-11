//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

import scala.collection.mutable.ListBuffer

import org.junit._
import org.junit.Assert._

class SignalTest {

  @Test def testSignalToSlot {
    val signal = Signal[Int]()
    val acc = ListBuffer[Int]()
    signal.onValue(acc += _)
    1 to 3 foreach signal.emit
    assertEquals(Seq(1, 2, 3), acc)
  }

  @Test def testOneShotSlot {
    val signal = Signal[Int]()
    val acc = ListBuffer[Int]()
    signal.onValue(acc += _).once()
    1 to 3 foreach signal.emit // slot should be removed after first emit
    assertEquals(Seq(1), acc)
  }

  @Test def testSlotPriority {
    val acc = new StringBuilder
    val signal = Signal[Unit]()
    // signals are notified highest to lowest by priority, then in registration order
    signal.onEmitAt(1)(acc append "C")
    signal.onEmitAt(3)(acc append "A")
    signal.onEmitAt(2)(acc append "B")
    signal.onEmitAt(1)(acc append "D") // same pri as C but registered after
    signal.emit()
    assertEquals("ABCD", acc.toString)
  }

  @Test def testAddDuringDispatch {
    val signal = Signal[Int]()
    val acc = ListBuffer[Int]()
    // connect a slot that will connect another slot when dispatched
    signal.onEmit(signal.onValue(acc += _).once())

    // this will connect our new signal but not dispatch to it
    signal.emit(5)
    assertEquals(0, acc.size)

    // now dispatch an event that should go to the added signal
    signal.emit(42)
    assertEquals(Seq(42), acc)
  }

  @Test def testRemoveDuringDispatch {
    val signal = Signal[Int]()
    val acc = ListBuffer[Int]()
    val rconn = signal.onValue(acc += _)

    // dispatch one event and make sure it's received
    signal.emit(5)
    assertEquals(Seq(5), acc)

    // now add our remover, and dispatch again
    signal.onEmit(rconn.close()) // use pri to ensure we're called first
    signal.emit(42)
    // since the accumulator will have been removed during this dispatch, it will receive the event
    // in question, even though the higher priority remover triggered first
    assertEquals(Seq(5, 42), acc)

    // finally dispatch one more event and make sure the accumulator didn't get it
    signal.emit(9)
    assertEquals(Seq(5, 42), acc)
  }

  @Test def testAddAndRemoveDuringDispatch {
    val signal = Signal[Int]()
    val remAcc = ListBuffer[Int]()
    val rconn = signal.onValue(remAcc += _)

    // dispatch one event and make sure it's received by the to-be-removed accumulator
    signal.emit(5)
    assertEquals(Seq(5), remAcc)

    // now add our adder/remover signal, and dispatch again
    val addAcc = ListBuffer[Int]()
    signal.onEmit { rconn.close() ; signal.onValue(addAcc += _)}
    signal.emit(42)
    // make sure removed got this event and added didn't
    assertEquals(Seq(5, 42), remAcc)
    assertEquals(0, addAcc.size)

    // finally emit one more and ensure that added got it and removed didn't
    signal.emit(9)
    assertEquals(Seq(9), addAcc)
    assertEquals(Seq(5, 42), remAcc)
  }

  @Test def testUnitSlot {
    val signal = Signal[Int]()
    var fired = false
    signal.onEmit(fired = true)
    signal.emit(42)
    assertTrue(fired)
  }

  @Test(expected=classOf[RuntimeException])
  def testSingleFailure {
    val signal = Signal[Unit]()
    signal.onEmit(throw new RuntimeException("Bang!"))
    signal.emit()
  }

  @Test(expected=classOf[ReactionException])
  def testMultiFailure {
    val signal = Signal[Unit]()
    signal.onEmit(throw new RuntimeException("Bing!"))
    signal.onEmit(throw new RuntimeException("Bang!"))
    signal.emit()
  }

  @Test def testMappedSignal {
    val signal = Signal[Int]()
    val mapped = signal.map(_.toString)

    var notifies = 0
    val c1 = mapped.onEmit(notifies += 1)
    val c2 = mapped.onValue(assertEquals("15", _))

    signal.emit(15)
    assertEquals(1, notifies)
    signal.emit(15)
    assertEquals(2, notifies)

    // disconnect from the mapped signal and ensure that it clears its connection
    c1.close()
    c2.close()
    assertFalse(signal.hasConnections)
  }
}
