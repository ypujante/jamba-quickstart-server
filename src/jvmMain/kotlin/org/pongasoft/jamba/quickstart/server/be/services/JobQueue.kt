package org.pongasoft.jamba.quickstart.server.be.services

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.linkedin.util.concurrent.WaitableCounter
import org.linkedin.util.lifecycle.ShutdownRequestedException
import org.linkedin.util.lifecycle.Shutdownable
import java.lang.IllegalStateException

/**
 * Implementation of a job queue using Rx. You add a job to the queue using <code>processJob(J)</code>.
 * The job will be processed via the <code>process</code> lambda and the result will be propagated
 * to the <code>results</code> stream if successful or the <code>errors</code> stream if an error
 * happened.
 */
class JobQueue<J, R>(process: (J) -> R,
                     val results: Observer<R>,
                     val errors: Observer<Throwable>? = null,
                     backPressureStrategy: BackpressureStrategy = BackpressureStrategy.BUFFER,
                     scheduler: Scheduler = Schedulers.newThread()) : Shutdownable {

  val MODULE = JobQueue::class.java.name!!
  val log = org.slf4j.LoggerFactory.getLogger(MODULE)!!

  /**
   * Keeps track of how many pending/executing jobs are in the queue and is also used for shutdown */
  private val _size = WaitableCounter()
  private var _shutdown = false
  private val _jobQueue = PublishSubject.create<J>()

  /**
   * Simple class to keep either a result or an error
   */
  private data class Result<R>(val result: R? = null, val error: Throwable? = null)

  /**
   * Creates a parallel Flowable to process the jobs
   */
  private val _processing: Flowable<Result<R>> =
      Flowable.create({ emitter: FlowableEmitter<J> ->

                        _jobQueue.subscribe(
                            {
                              // handle next job
                              emitter.onNext(it)
                            },
                            {
                              // handle error
                              emitter.onError(it)
                            },
                            {
                              // handle completion
                              emitter.onComplete()
                            }
                        )
                      }, backPressureStrategy)
          .parallel()
          .runOn(scheduler)
          .map { job: J ->
            try {
              Result(result = process(job))
            } catch (th: Throwable) {
              Result<R>(error = th)
            }
          }
          .sequential() // we join after parallel

  init {
    // subscribe to processing to handle results (dispatch to <code>results</code> or <code>errors</code>)
    _processing.subscribe { result: Result<R> ->
      try {
        if (result.result != null)
          results.onNext(result.result)

        if (result.error != null)
          errors?.onNext(result.error)

      } finally {
        _size.dec()
      }
    }
  }

  /**
   * How many items in the queue (either pending or being processed)
   */
  val size: Int
    get() = _size.counter

  /**
   * Enqueues a job for processing (asynchronous processing)
   */
  fun processJob(job: J) {
    synchronized(this) {
      if (_shutdown)
        throw ShutdownRequestedException(MODULE)

      _size.inc()
      _jobQueue.onNext(job)
    }
  }

  /**
   * This methods sets the entity in shutdown mode. Any method call on this
   * entity after shutdown should be either rejected
   * (`IllegalStateException`) or discarded. This method should
   * not block and return immediately.
   *
   * @see waitForShutdown
   */
  override fun shutdown() {
    synchronized(this) {
      _shutdown = true
      _jobQueue.onComplete()
    }
  }

  /**
   * Waits for shutdown to be completed. After calling shutdown, there may
   * still be some pending work that needs to be accomplished. This method
   * will block until it is done.
   *
   * @exception InterruptedException if interrupted while waiting
   * @exception IllegalStateException if shutdown has not been called
   */
  override fun waitForShutdown() {
    _size.waitForCounter()
  }

  /**
   * Waits for shutdown to be completed. After calling shutdown, there may
   * still be some pending work that needs to be accomplished. This method
   * will block until it is done but no longer than the timeout.
   *
   * @param timeout how long to wait maximum for the shutdown
   * (see [ClockUtils.toTimespan])
   * @exception InterruptedException if interrupted while waiting
   * @exception IllegalStateException if shutdown has not been called
   * @exception TimeoutException if shutdown still not complete after timeout
   */
  override fun waitForShutdown(timeout: Any?) {
    _size.waitForCounter(timeout)
  }
}