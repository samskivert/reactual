//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

import org.junit._
import org.junit.Assert._

class OptValueTest {

  @Test def testNonEmptyValue {
    val value = OptValue[Int](42)
    assertTrue(value.isDefined)
    assertFalse(value.isEmpty)

    var fired = false
    value.onValue({ nv =>
      assertEquals(Some(15), nv)
      fired = true
    }).once()
    value.update(15)
    assertEquals(15, value.get)
    assertTrue(fired)

    var cleared = false
    value.onValue({ nv =>
      assertEquals(None, nv)
      cleared = true
    }).once()
    value.clear()
    assertTrue(value.isEmpty)
    assertTrue(cleared)
  }

  @Test def testEmptyValue {
    val value = OptValue[Int]()
    assertFalse(value.isDefined)
    assertTrue(value.isEmpty)

    var fired = false
    value.onValue { nv =>
      assertEquals(Some(15), nv)
      fired = true
    }
    value.update(15)
    assertEquals(15, value.get)
    assertTrue(fired)
  }

  @Test def testNonRepeat {
    val value = OptValue[Int]()
    var sets = 0
    var clears = 0
    value.onValue { _ match {
      case None => clears += 1
      case Some(v) => sets += 1
    }}
    value.clear()
    assertEquals(0, clears)
    value.update(42)
    assertEquals(1, sets)
    value.update(42)
    assertEquals(1, sets)
    value.clear()
    assertEquals(1, clears)
    value.clear()
    assertEquals(1, clears)
  }

  @Test def testDisallowNull {
    val value = OptValue[String]()
    try {
      value.update(null)
      fail("Expected IllegalArgumentExeption")
    } catch {
      case x :IllegalArgumentException => // yay!
    }
  }
}
