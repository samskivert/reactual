//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

import java.io.{PrintStream, PrintWriter}

/** Communicates failures from one or more reactors. */
class ReactionException (val failures :Seq[Throwable]) extends RuntimeException {

  override def getMessage = {
    val buf = new StringBuilder
    for (failure <- failures) {
      if (buf.length > 0) buf.append(", ")
      buf.append(failure.getClass.getName).append(": ").append(failure.getMessage)
    }
    failures.size + " failures: " + buf
  }

  override def printStackTrace (s :PrintStream) {
    failures foreach { _.printStackTrace(s) }
  }

  override def printStackTrace (w :PrintWriter) {
    failures foreach { _.printStackTrace(w) }
  }

  override def fillInStackTrace () :Throwable = this // no stack trace here
}
