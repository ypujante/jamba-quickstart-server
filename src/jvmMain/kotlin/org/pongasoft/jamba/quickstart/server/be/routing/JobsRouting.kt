package org.pongasoft.jamba.quickstart.server.be.routing

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.pongasoft.jamba.quickstart.server.be.Beans

/*
  Check Locations: https://ktor.io/servers/features/locations.html for building url
 */

fun Application.jobsRouting(beans: Beans = Beans.default) {
  val log = environment.log
  log.info("Initializing Routing => $beans")
  routing {
    route("/api/v1/jobs") {
      get {
        log.info("GET /jobs")
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
