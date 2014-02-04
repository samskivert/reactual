//
// Reactual - an FRP-ish library for Scala
// Copyright (c) 2013, Michael Bayne - All rights reserved.
// http://github.com/samskivert/reactual/blob/master/LICENSE

package reactual

import org.junit._
import org.junit.Assert._

class FutureTest {

  class Counter extends (Any => Unit) {
    var count = 0
    def apply (any :Any) { count += 1 }
    def assertTriggered (count :Int) = assertEquals(count, this.count)
    def assertTriggered (message :String, count :Int) = assertEquals(message, count, this.count)
    def reset () = count = 0
  }

  class FutureCounter {
    val successes = new Counter()
    val failures = new Counter()
    val completes = new Counter()

    def bind (future :Future[_]) {
      reset()
      future.onSuccess(successes)
      future.onFailure(failures)
      future.onComplete(completes)
    }

    def check (state :String, scount :Int, fcount :Int, ccount :Int) {
      successes.assertTriggered("Successes " + state, scount)
      failures.assertTriggered("Failures " + state, fcount)
      completes.assertTriggered("Completes " + state, ccount)
    }

    def reset () {
      successes.reset()
      failures.reset()
      completes.reset()
    }
  }

  @Test def testImmediate () {
    val counter = new FutureCounter()
    val success = Future.success("Yay!")
    counter.bind(success)
    counter.check("immediate succeed", 1, 0, 1)

    val failure = Future.failure(new Exception("Boo!"))
    counter.bind(failure)
    counter.check("immediate failure", 0, 1, 1)
  }

  @Test def testDeferred () {
    val counter = new FutureCounter()
    val success = Promise[String]()
    counter.bind(success)
    counter.check("before succeed", 0, 0, 0)
    success.succeed("Yay!")
    counter.check("after succeed", 1, 0, 1)

    val failure = Promise[String]()
    counter.bind(failure)
    counter.check("before fail", 0, 0, 0)
    failure.fail(new Exception("Boo!"))
    counter.check("after fail", 0, 1, 1)

    assertFalse(success.hasConnections)
    assertFalse(failure.hasConnections)
  }

  @Test def testMappedImmediate () {
    val counter = new FutureCounter()

    val success = Future.success("Yay!")
    counter.bind(success.map(_ != null))
    counter.check("immediate succeed", 1, 0, 1)

    val failure = Future.failure[String](new Exception("Boo!"))
    counter.bind(failure.map(_ != null))
    counter.check("immediate failure", 0, 1, 1)
  }

  @Test def testMappedDeferred () {
    val counter = new FutureCounter()

    val success = Promise[String]()
    counter.bind(success.map(_ != null))
    counter.check("before succeed", 0, 0, 0)
    success.succeed("Yay!")
    counter.check("after succeed", 1, 0, 1)

    val failure = Promise[String]()
    counter.bind(failure.map(_ != null))
    counter.check("before fail", 0, 0, 0)
    failure.fail(new Exception("Boo!"))
    counter.check("after fail", 0, 1, 1)

    assertFalse(success.hasConnections)
    assertFalse(failure.hasConnections)
  }

  val successMap = (value :String) => Future.success(value != null)
  val failMap = (value :String) => Future.failure(new Exception("Barzle!"))

  @Test def testFlatMappedImmediate () {
    val (scounter, fcounter) = (new FutureCounter(), new FutureCounter())

    val success = Future.success("Yay!")
    scounter.bind(success.flatMap(successMap))
    fcounter.bind(success.flatMap(failMap))
    scounter.check("immediate success/success", 1, 0, 1)
    fcounter.check("immediate success/failure", 0, 1, 1)

    val failure = Future.failure[String](new Exception("Boo!"))
    scounter.bind(failure.flatMap(successMap))
    fcounter.bind(failure.flatMap(failMap))
    scounter.check("immediate failure/success", 0, 1, 1)
    scounter.check("immediate failure/failure", 0, 1, 1)
  }

  @Test def testFlatMappedDeferred () {
    val (scounter, fcounter) = (new FutureCounter(), new FutureCounter())

    val success = Promise[String]()
    scounter.bind(success.flatMap(successMap))
    scounter.check("before succeed/succeed", 0, 0, 0)
    fcounter.bind(success.flatMap(failMap))
    fcounter.check("before succeed/fail", 0, 0, 0)
    success.succeed("Yay!")
    scounter.check("after succeed/succeed", 1, 0, 1)
    fcounter.check("after succeed/fail", 0, 1, 1)

    val failure = Promise[String]()
    scounter.bind(failure.flatMap(successMap))
    fcounter.bind(failure.flatMap(failMap))
    scounter.check("before fail/success", 0, 0, 0)
    fcounter.check("before fail/failure", 0, 0, 0)
    failure.fail(new Exception("Boo!"))
    scounter.check("after fail/success", 0, 1, 1)
    fcounter.check("after fail/failure", 0, 1, 1)

    assertFalse(success.hasConnections)
    assertFalse(failure.hasConnections)
  }

  @Test def testFlatMappedDoubleDeferred () {
    val (scounter, fcounter) = (new FutureCounter(), new FutureCounter())

    {
      val success = Promise[String]()
      val innerSuccessSuccess = Promise[Boolean]()
      scounter.bind(success.flatMap(_ => innerSuccessSuccess))
      scounter.check("before succeed/succeed", 0, 0, 0)
      val innerSuccessFailure = Promise[Boolean]()
      fcounter.bind(success.flatMap(_ => innerSuccessFailure))
      fcounter.check("before succeed/fail", 0, 0, 0)

      success.succeed("Yay!")
      scounter.check("after first succeed/succeed", 0, 0, 0)
      fcounter.check("after first succeed/fail", 0, 0, 0)
      innerSuccessSuccess.succeed(true)
      scounter.check("after second succeed/succeed", 1, 0, 1)
      innerSuccessFailure.fail(new Exception("Boo hoo!"))
      fcounter.check("after second succeed/fail", 0, 1, 1)

      assertFalse(success.hasConnections)
      assertFalse(innerSuccessSuccess.hasConnections)
      assertFalse(innerSuccessFailure.hasConnections)
    }

    {
      val failure = Promise[String]()
      val innerFailureSuccess = Promise[Boolean]()
      scounter.bind(failure.flatMap(_ => innerFailureSuccess))
      scounter.check("before fail/succeed", 0, 0, 0)
      val innerFailureFailure = Promise[Boolean]()
      fcounter.bind(failure.flatMap(_ => innerFailureFailure))
      fcounter.check("before fail/fail", 0, 0, 0)

      failure.fail(new Exception("Boo!"))
      scounter.check("after first fail/succeed", 0, 1, 1)
      fcounter.check("after first fail/fail", 0, 1, 1)
      innerFailureSuccess.succeed(true)
      scounter.check("after second fail/succeed", 0, 1, 1)
      innerFailureFailure.fail(new Exception("Is this thing on?"))
      fcounter.check("after second fail/fail", 0, 1, 1)

      assertFalse(failure.hasConnections)
      assertFalse(innerFailureSuccess.hasConnections)
      assertFalse(innerFailureFailure.hasConnections)
    }
  }

  @Test def testSequenceImmediate () {
    val counter = new FutureCounter()

    val (success1, success2) = (Future.success("Yay 1!"), Future.success("Yay 2!"))
    val failure1 = Future.failure[String](new Exception("Boo 1!"))
    val failure2 = Future.failure[String](new Exception("Boo 2!"))

    val sucseq = Future.sequence(Seq(success1, success2))
    counter.bind(sucseq)
    sucseq.onSuccess(results => assertEquals(Seq("Yay 1!", "Yay 2!"), results))
    counter.check("immediate seq success/success", 1, 0, 1)

    counter.bind(Future.sequence(Seq(success1, failure1)))
    counter.check("immediate seq success/failure", 0, 1, 1)

    counter.bind(Future.sequence(Seq(failure1, success2)))
    counter.check("immediate seq failure/success", 0, 1, 1)

    counter.bind(Future.sequence(Seq(failure1, failure2)))
    counter.check("immediate seq failure/failure", 0, 1, 1)
  }

  @Test def testSequenceDeferred () {
    val counter = new FutureCounter()

    val (success1, success2) = (Promise[String](), Promise[String]())
    val (failure1, failure2) = (Promise[String](), Promise[String]())

    val suc2seq = Future.sequence(Seq(success1, success2))
    counter.bind(suc2seq)
    suc2seq.onSuccess(results => assertEquals(Seq("Yay 1!", "Yay 2!"), results))
    counter.check("before seq succeed/succeed", 0, 0, 0)
    success1.succeed("Yay 1!")
    success2.succeed("Yay 2!")
    counter.check("after seq succeed/succeed", 1, 0, 1)

    val sucfailseq = Future.sequence(Seq(success1, failure1))
    sucfailseq.onFailure { cause =>
      assertTrue(cause.isInstanceOf[ReactionException])
      assertEquals("1 failures: java.lang.Exception: Boo 1!", cause.getMessage)
    }
    counter.bind(sucfailseq)
    counter.check("before seq succeed/fail", 0, 0, 0)
    failure1.fail(new Exception("Boo 1!"))
    counter.check("after seq succeed/fail", 0, 1, 1)

    val failsucseq = Future.sequence(Seq(failure1, success2))
    failsucseq.onFailure { cause =>
      assertTrue(cause.isInstanceOf[ReactionException])
      assertEquals("1 failures: java.lang.Exception: Boo 1!", cause.getMessage)
    }
    counter.bind(failsucseq)
    counter.check("after seq fail/succeed", 0, 1, 1)

    val fail2seq = Future.sequence(Seq(failure1, failure2))
    fail2seq.onFailure { cause =>
      assertTrue(cause.isInstanceOf[ReactionException])
      assertEquals("2 failures: java.lang.Exception: Boo 1!, java.lang.Exception: Boo 2!",
                   cause.getMessage)
    }
    counter.bind(fail2seq)
    counter.check("before seq fail/fail", 0, 0, 0)
    failure2.fail(new Exception("Boo 2!"))
    counter.check("after seq fail/fail", 0, 1, 1)
  }

  @Test def testSequenceEmpty () {
    val counter = new FutureCounter()
    val seq = Future.sequence(Seq())
    counter.bind(seq)
    counter.check("sequence empty list succeeds", 1, 0, 1)
  }

  @Test def testCollectEmpty () {
    val counter = new FutureCounter()
    val seq = Future.collect(Seq())
    counter.bind(seq)
    counter.check("collect empty list succeeds", 1, 0, 1)
  }

  @Test def testCollectImmediate () {
    val counter = new FutureCounter()

    val (success1, success2) = (Future.success("Yay 1!"), Future.success("Yay 2!"))
    val failure1 = Future.failure[String](new Exception("Boo 1!"))
    val failure2 = Future.failure[String](new Exception("Boo 2!"))

    val sucseq = Future.collect(Seq(success1, success2))
    counter.bind(sucseq)
    sucseq.onSuccess(results => assertEquals(Seq("Yay 1!", "Yay 2!"), results))
    counter.check("immediate seq success/success", 1, 0, 1)

    val sucfail = Future.collect(Seq(success1, failure1))
    counter.bind(sucfail)
    sucfail.onSuccess(results => assertEquals(Seq("Yay 1!"), results))
    counter.check("immediate seq success/failure", 1, 0, 1)

    val failsuc = Future.collect(Seq(failure1, success2))
    counter.bind(failsuc)
    failsuc.onSuccess(results => assertEquals(Seq("Yay 2!"), results))
    counter.check("immediate seq failure/success", 1, 0, 1)

    val failfail = Future.collect(Seq(failure1, failure2))
    counter.bind(failfail)
    failfail.onSuccess(results => assertEquals(Seq(), results))
    counter.check("immediate seq failure/failure", 1, 0, 1)
  }
}
