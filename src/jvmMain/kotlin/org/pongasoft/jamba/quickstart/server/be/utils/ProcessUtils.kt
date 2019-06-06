package org.pongasoft.jamba.quickstart.server.be.utils

import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

data class ProcessResult(val exitValue: Int)
data class ProcessOutResult<out U>(val exitValue: Int, val stdout: U)

/**
 * Returns the exit value of the process as a CompletionStage
 */
fun Process.asyncResult(executor: Executor = ForkJoinPool.commonPool()): CompletionStage<ProcessResult> {
  return CompletableFuture.supplyAsync(Supplier { ProcessResult(this.waitFor()) }, executor)
}

/**
 * Returns the exit value and the processing of stdout of the process as a CompletionStage
 */
fun <U> Process.asyncStream(stdoutProcessor: (InputStream) -> U,
                            executor: Executor = ForkJoinPool.commonPool()): CompletionStage<ProcessOutResult<U>> {


  if (this.inputStream == null)
    throw IllegalStateException("stdout has been redirected")
  else {
    val exitValueFuture = this.asyncResult(executor)

    val stdoutFuture =
        CompletableFuture.supplyAsync(Supplier { this.inputStream.use { stdoutProcessor(it) } },
                                      executor)

    return stdoutFuture
        .thenCombine(exitValueFuture) { u, (exitValue) -> ProcessOutResult(exitValue, u) }
  }
}

/**
 * Returns the exit value and output as a CompletionStage
 */
fun Process.asyncOut(executor: Executor = ForkJoinPool.commonPool(),
                     charset: Charset = Charsets.UTF_8): CompletionStage<ProcessOutResult<String>> {
  return this.asyncStream({ it.bufferedReader(charset).readText() }, executor)
}

/**
 * Returns the exit value and output as result (blocking call)
 */
fun Process.syncOut(executor: Executor = ForkJoinPool.commonPool(),
                    charset: Charset = Charsets.UTF_8,
                    timeout: Long? = null,
                    unit: TimeUnit = TimeUnit.MILLISECONDS): ProcessOutResult<String> {

  val future = this.asyncOut(executor, charset).toCompletableFuture()

  return if (timeout != null)
    future.get(timeout, unit)
  else
    future.get()
}


