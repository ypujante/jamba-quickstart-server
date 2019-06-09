package org.pongasoft.jamba.quickstart.server.be.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.RequestConnectionPoint
import io.ktor.request.receiveParameters
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.pongasoft.jamba.quickstart.server.be.services.JobRun
import org.pongasoft.jamba.quickstart.server.be.services.JobStatus
import org.pongasoft.jamba.quickstart.server.be.services.JobsMgr

/*
  Check Locations: https://ktor.io/servers/features/locations.html for building url (experimental feature)
 */

fun Application.jobsAPI(jobsMgr: JobsMgr) {
  val log = environment.log
  routing {
    route(API_URL) {
      // POST /jobs => createJob
      post("/jobs") {
        log.info("POST /jobs")
        val params = call.receiveParameters().entries().associateBy({ e -> e.key }, { e -> e.value.first() })
        val run = jobsMgr.enqueueJob(params)
        call.respond(buildJob(run, call.request.origin))
      }

      /**
       * GET /job/{jobId} => returns uri/info/state
       */
      get("/job/{jobId}") {
        val jobId = call.parameters["jobId"]!! // this path will not match otherwise
        val run = jobsMgr.findJobRun(jobId)
        if (run != null) {
          call.respond(buildJob(run, call.request.origin))
        } else {
          call.respond(HttpStatusCode.NotFound)
        }
      }

      /**
       * GET /job/{jobId}/result/{name} => getZipFile
       */
      get("/job/{jobId}/result/{name}") {
        val jobId = call.parameters["jobId"]!!
        val name = call.parameters["name"]!!

        val run = jobsMgr.findJobRun(jobId)
        if (run?.result != null) {
          call.response.header(HttpHeaders.ContentDisposition,
                               ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName,
                                                                           name).toString())
          call.respondFile(run.result)
        } else {
          call.respond(HttpStatusCode.NotFound)
        }
      }
    }
  }
}

/**
 * Builds the result (will be converted to json)
 */
private fun buildJob(jobRun: JobRun, origin: RequestConnectionPoint): Job {
  val job = Job()

  job.jobId = jobRun.jobId
  job.uri = getJobURI(jobRun, origin)

  job.createdTime = jobRun.createdTime
  job.startedTime = jobRun.startedTime
  job.lastUpdatedTime = jobRun.lastUpdatedTime
  job.resultURI = getZipFileURI(jobRun, origin)
  job.error = jobRun.error?.message

  if (jobRun.jobStatus == JobStatus.COMPLETED)
    job.completionStatus = if (jobRun.error == null) JobCompletionStatus.OK else JobCompletionStatus.ERROR

  return job
}
