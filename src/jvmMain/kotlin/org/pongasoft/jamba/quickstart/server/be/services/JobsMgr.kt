package org.pongasoft.jamba.quickstart.server.be.services

import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.linkedin.util.lifecycle.Destroyable
import org.pongasoft.jamba.quickstart.server.be.utils.child
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.time.Clock
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap
import kotlin.concurrent.withLock

/**
 * Defines the interface of a jobs mgr
 */
interface JobsMgr {
  /**
   * Enqueues the job to be completed */
  fun enqueueJob(plugin: Map<String, String>): JobRun

  /**
   * returns the job run  */
  fun findJobRun(jobId: String): JobRun?
}

/**
 * Status for a job
 */
enum class JobStatus {
  NOT_STARTED,
  RUNNING,
  COMPLETED
}

/**
 * Maintains the state of a job while running
 */
data class JobRun(val jobId: String,
                  val createdTime: Long,
                  val lastUpdatedTime: Long = createdTime,
                  val startedTime: Long? = null,
                  val jobStatus: JobStatus = JobStatus.NOT_STARTED,
                  val result: File? = null,
                  val error: Throwable? = null)

/**
 * Represents the task that will/can be executed to accomplish the work
 */
private class Job(val jobId: String,
                  val plugin: Map<String, String>,
                  val blankPluginMgr: BlankPluginMgr,
                  val clock: Clock) {

  val MODULE = Job::class.java.name!!
  val log = org.slf4j.LoggerFactory.getLogger(MODULE)!!

  var jobRun = JobRun(jobId, clock.millis())

  fun execute(): JobRun {

    val now = clock.millis()
    jobRun = jobRun.copy(startedTime = now, lastUpdatedTime = now, jobStatus = JobStatus.RUNNING)

    try {
      val zipFile = generateZipFile()
      jobRun = jobRun.copy(jobStatus = JobStatus.COMPLETED, lastUpdatedTime = clock.millis(), result = zipFile)
      log.info("Generated zip file ${zipFile.path}")
    } catch (th: Throwable) {
      jobRun = jobRun.copy(jobStatus = JobStatus.COMPLETED, lastUpdatedTime = clock.millis(), error = th)
    }

    return jobRun
  }

  private fun generateZipFile(): File {
    val dir = Files.createTempDirectory(jobId)

    val name = plugin["name"] ?: "Plugin"

    return blankPluginMgr.generateBlankPlugin(dir.toFile().child("$name-src.zip"), plugin)
  }
}

/**
 * Manages job */
class JobsMgrImpl(private val clock: Clock,
                  private val blankPluginMgr: BlankPluginMgr) : JobsMgr, Destroyable {

  val MODULE = JobsMgr::class.java.name!!
  val log = org.slf4j.LoggerFactory.getLogger(MODULE)!!

  private val _jobs = HashMap<String, Job>()
  private val _lock = ReentrantLock()

  private val _results = PublishSubject.create<JobRun>()
  private val _errors = PublishSubject.create<Throwable>()

  private val _jobQueue = JobQueue<Job, JobRun>(process = { it.execute() },
                                                results = _results,
                                                errors = _errors)

  /**
   * init => listens to tasks and execute them in parallel
   */
  init {
    _results.observeOn(Schedulers.newThread()).delay(1, TimeUnit.MINUTES).subscribe { run ->
      log.info("Removing jobRun ${run.jobId}")
      _lock.withLock { _jobs.remove(run.jobId) }
      cleanup(run)
    }

    _errors.subscribe {
      log.warn("Unexpected exception during processing (ignored)", it)
    }

    log.info("JobMgr - init complete")
  }

  /**
   * Enqueues the job to be completed */
  override fun enqueueJob(plugin: Map<String, String>): JobRun {
    val time = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date(clock.millis()))
    val jobId = time + "-" + UUID.randomUUID().toString()

    val job = Job(jobId, plugin, blankPluginMgr, clock)

    return _lock.withLock {
      _jobs.put(jobId, job)
      log.info("Submitting job ${jobId}")
      _jobQueue.processJob(job)
      job.jobRun
    }
  }

  /**
   * returns the job run  */
  override fun findJobRun(jobId: String): JobRun? {
    return _lock.withLock {
      _jobs[jobId]?.jobRun
    }
  }

  /**
   * Delete the files generated during a run  */
  private fun cleanup(run: JobRun) {
    run.result?.let { file ->
      try {
        Files.deleteIfExists(file.toPath())
        log.info("deleting ${file.parentFile.canonicalPath}")
        Files.deleteIfExists(file.parentFile.toPath())
      } catch (th: Throwable) {
        log.warn("Exception while cleaning up files", th)
      }
    }
  }

  /**
   * This method destroys the entity, cleaning up any resource that needs to
   * be cleaned up, like closing files, database connection..  */
  override fun destroy() {
    log.info("JobMgr - destroy")
    _jobQueue.shutdown()
    _lock.withLock {
      _jobs.values.forEach { cleanup(it.jobRun) }
      _jobs.clear()
    }
  }

}
