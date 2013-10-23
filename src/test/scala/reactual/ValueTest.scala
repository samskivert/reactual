//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

import org.junit._
import org.junit.Assert._

class ValueTest {

  @Test def testSimpleListener {
    val value = Value(42)
    var fired = false
    value.onValue { nv =>
      assertEquals(15, nv)
      fired = true
    }
    assertEquals(42, value.update(15))
    assertEquals(15, value.get)
    assertTrue(fired)
  }

  @Test def testAsSignal {
    val value = Value(42)
    var fired = false
    value.onValue { nv =>
      assertEquals(15, nv)
      fired = true
    }
    value.update(15)
    assertTrue(fired)
  }

  @Test def testAsOnceSignal {
    val value = Value(42)
    var counter = 0
    value.onEmit(counter += 1).once()
    value.update(15)
    value.update(42)
    assertEquals(1, counter)
  }

  @Test def testMappedValue {
    val value = Value(42)
    val mapped = value.map(_.toString)

    var counter = 0
    val c1 = mapped.onEmit(counter += 1)
    val c2 = mapped.onValue(assertEquals("15", _))

    value.update(15)
    assertEquals(1, counter)
    value.update(15)
    assertEquals(1, counter)
    value.updateForce(15)
    assertEquals(2, counter)

    // disconnect from the mapped value and ensure that it disconnects in turn
    c1.close()
    c2.close()
    assertFalse(value.hasConnections)
  }

  @Test def testConnectNotify {
    val value = Value(42)
    var fired = false
    value.onValueNotify { v =>
      assertEquals(42, v)
      fired = true
    }
    assertTrue(fired)
  }

  @Test def testDisconnectInNotfy {
    val value = Value(42)
    var fired = 0
    var conn :Connection = null
    conn = value.onEmit {
      fired += 1
      conn.close()
    }
    value.update(12)
    value.update(25)
    assertEquals("Disconnecting during dispatch works", 1, fired)
    conn.close() // check no freakouty when disconnecting disconnected connection
  }

  @Test def testDisconnectBeforeNotify {
    val value = Value(42)
    var fired = 0
    value.onEmit(fired += 1).close()
    value.update(15)
    assertEquals("Disconnecting before geting an update still disconnects", 0, fired)
  }
}
