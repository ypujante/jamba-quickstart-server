package org.pongasoft.jamba.quickstart.server.be.api

import io.ktor.application.Application
import io.ktor.http.RequestConnectionPoint
import org.linkedin.util.url.URLBuilder
import org.pongasoft.jamba.quickstart.server.be.Beans
import org.pongasoft.jamba.quickstart.server.be.services.JobRun
import java.net.URI

const val API_URL = "/api/v1"

/**
 * How the job was completed */
enum class JobCompletionStatus {
  NONE,
  OK,
  ERROR
}

data class Job(var jobId: String? = null,
               var uri: URI? = null,
               var createdTime: Long? = null,
               var startedTime: Long? = null,
               var lastUpdatedTime: Long? = null,
               var completionStatus: JobCompletionStatus = JobCompletionStatus.NONE,
               var resultURI: URI? = null,
               var error: String? = null)

/**
 * Main api: installs all other apis
 */
fun Application.api(beans: Beans) {
  jobsAPI(beans.jobsMgr)
  adminAPI(beans.adminUserName, beans.adminPassword, beans.blankPluginMgr)
}

internal fun RequestConnectionPoint.url(path: String) : URLBuilder {
  val url = URLBuilder.createFromPath(path)
  url.scheme = scheme
  if((scheme == "http" && port != 80) || (scheme == "https" && port != 443))
    url.port = port
  url.host = host
  return url
}

/**
 * Returns the URI for getting a job
 */
internal fun getJobURI(run: JobRun, origin: RequestConnectionPoint) : URI {
  return origin.url(API_URL)
      .appendPath("job")
      .appendPath(run.jobId)
      .toJavaURL().toURI()
}

/**
 * Returns the URI for getting the zip file
 */
internal fun getZipFileURI(run: JobRun, origin: RequestConnectionPoint) : URI? {
  return run.result?.let { r ->
    origin.url(API_URL)
        .appendPath("job")
        .appendPath(run.jobId)
        .appendPath("result")
        .appendPath(r.name)
        .toJavaURL().toURI()
  }
}