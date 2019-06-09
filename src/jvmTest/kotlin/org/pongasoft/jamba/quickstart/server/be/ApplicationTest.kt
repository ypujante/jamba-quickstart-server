package org.pongasoft.jamba.quickstart.server.be

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SettableClock
import org.linkedin.util.lifecycle.Destroyable
import org.pongasoft.jamba.quickstart.server.be.api.API_URL
import org.pongasoft.jamba.quickstart.server.be.api.Job
import org.pongasoft.jamba.quickstart.server.be.api.JobCompletionStatus
import org.pongasoft.jamba.quickstart.server.be.services.JobRun
import org.pongasoft.jamba.quickstart.server.be.services.JobStatus
import org.pongasoft.jamba.quickstart.server.be.services.JobsMgr
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * This test is designed for the api level
 */
class ApplicationTest {

  val clock = SettableClock()

  data class MyJobsMgr(val clock: Clock, var destroyed : Boolean = false) : JobsMgr, Destroyable {

    private val _jobs = mutableMapOf<String, JobRun>()

    override fun enqueueJob(plugin: Map<String, String>): JobRun {
      val jobId = plugin["name"] ?: error("name not found")
      val jobRun = JobRun(jobId = jobId, createdTime = clock.currentTimeMillis())
      _jobs.put(jobId, jobRun)
      return jobRun
    }

    override fun findJobRun(jobId: String): JobRun? {
      return _jobs[jobId]
    }

    override fun destroy() { destroyed = true }

    fun completeJob(jobId: String): JobRun? {
      val run = findJobRun(jobId)

      if(run != null)
      {
        val newJob = run.copy(startedTime = clock.currentTimeMillis(),
                              jobStatus = JobStatus.COMPLETED,
                              result = File("/tmp/${jobId}.zip"))
        _jobs[jobId] = newJob
        return newJob
      }

      return null
    }
  }

  val jobsMgr = MyJobsMgr(clock)
  val beans = Beans(jobsMgr = jobsMgr)

  /**
   * Test for POST /jobs (add a job
   */
  @Test fun testJobsPost() {
    withTestApplication({
                          initServer(beans)
                        }) {

      // jobs mgr not destroyed yet
      assertFalse(jobsMgr.destroyed)

      val jobId = "MyPlugin"

      // GET /job/MyPlugin => not found (does not exists yet)
      with(handleRequest(HttpMethod.Get, "$API_URL/job/$jobId")) {
        assertEquals(HttpStatusCode.NotFound, response.status())
      }

      var expectedJob = Job(jobId = jobId,
                            createdTime = clock.currentTimeMillis(),
                            lastUpdatedTime = clock.currentTimeMillis(),
                            uri = URI.create("http://localhost$API_URL/job/$jobId"))

      val mapper = jacksonObjectMapper()

      // POST /jobs
      handleRequest(HttpMethod.Post, "$API_URL/jobs") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
        setBody(listOf("name" to jobId).formUrlEncode())
      }.apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(expectedJob, mapper.readValue(response.content.toString()))
      }

      // GET /job/MyPlugin => found now exists
      with(handleRequest(HttpMethod.Get, "$API_URL/job/$jobId")) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(expectedJob, mapper.readValue(response.content.toString()))
      }

      // now we complete the job
      jobsMgr.completeJob(jobId)

      expectedJob = expectedJob.copy(startedTime = clock.currentTimeMillis(),
                                     completionStatus = JobCompletionStatus.OK,
                                     resultURI = URI.create("http://localhost$API_URL/job/$jobId/result/$jobId.zip"))

      // GET /job/MyPlugin => found now exists
      with(handleRequest(HttpMethod.Get, "$API_URL/job/$jobId")) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(expectedJob, mapper.readValue(response.content.toString()))
      }
    }

    // when we exit, we make sure that the lifecycle (registered in initServer) calls the destroy method on the
    // manager
    assertTrue(jobsMgr.destroyed)
  }
}