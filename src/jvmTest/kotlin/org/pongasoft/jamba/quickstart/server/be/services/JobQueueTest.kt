package org.pongasoft.jamba.quickstart.server.be.services

import io.reactivex.subjects.PublishSubject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.linkedin.util.clock.ClockUtils
import org.linkedin.util.concurrent.ThreadControl
import java.util.*

class JobQueueTest {

  @Test
  fun testJobQueue() {
    // will collect the results and errors
    val results = PublishSubject.create<String>()
    val errors = PublishSubject.create<Throwable>()

    // handle timing
    val tc = ThreadControl(ClockUtils.toTimespan("2s"))

    val jobs = (1..4)

    // job queue which will simply return what tc.blockWithException returns
    val queue =
        JobQueue<Int, String>(
            process = { job ->
              println("${Thread.currentThread()} | ${Date()} | processing => $job")
              tc.blockWithException(job).toString()
            },
            results = results,
            errors = errors)

    val exception = RuntimeException("p2")

    // we need to subscribe to results and errors BEFORE processing any job!
    results.subscribe {
      tc.block(it)
      println("${Thread.currentThread()} | ${Date()} | result -> ${it}.")
    }

    errors.subscribe{
      tc.block("p2")
      assertEquals(it, exception)
      println("${Thread.currentThread()} | ${Date()} | error -> ${it}.")
    }

    // start to process every job
    jobs.forEach { i ->
      println("${Thread.currentThread()} | ${Date()} | sync submitting ${i}")
      queue.processJob(i)
    }

    // this is effectively testing that the jobs are all started in parallel
    jobs.forEach { i ->
      tc.waitForBlock(i)
    }

    // we should have started the right number of jobs
    assertEquals(jobs.count(), queue.size)

    // unblock job 3 and make sure we are receiving "results" (results.onNext is single thread)
    tc.unblock(3, "p3")
    tc.waitForBlock("p3")

    // unblock 2 with an exception which will propagate to "errors"
    tc.unblock(2, exception)

    // let p3 complete
    tc.unblock("p3")

    // wait and unblock "p2" (in "errors" queue)
    tc.waitForBlock("p2")
    tc.unblock("p2")

    // finally make sure 1 and 4 goes through the entire process
    tc.unblock(4, "p4")
    tc.waitForBlock("p4")

    tc.unblock(1, "p1")
    tc.unblock("p4")
    tc.unblock("p1")

    println("${Thread.currentThread()} | ${Date()} | done.")

    queue.shutdown()

    // if anything is pending at this point it will raise an exception
    queue.waitForShutdown("5s")

  }


}