package org.pongasoft.jamba.quickstart.server.be.routing

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import org.pongasoft.jamba.quickstart.server.be.beans

/*
  Check Locations: https://ktor.io/servers/features/locations.html for building url
 */

@KtorExperimentalAPI
fun Application.jobsRouting() {
  val log = environment.log
  routing {
    route("/api/v1/jobs") {
      get {
        log.info("GET /jobs")
        val beans = call.beans
        log.info("beans => ${beans}")
        call.respondText("get /jobs ...")
      }
      post {
        log.info("POST /jobs")
        call.respondText("post /jobs")
      }
    }
  }
}