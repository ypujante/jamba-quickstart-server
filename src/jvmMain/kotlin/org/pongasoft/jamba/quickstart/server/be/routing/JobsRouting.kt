package org.pongasoft.jamba.quickstart.server.be.routing

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun Application.jobsRouting() {
    environment.log.info("property -> ${environment.config.propertyOrNull("org.pongasoft.jamba.quickstart.server.staticWebDir")?.getString()}")
    routing {
        route("/api/v1/jobs") {
            get {
                environment.log.info("GET /jobs")
                call.respondText("get /jobs ...")
            }
            post {
                environment.log.info("POST /jobs")
                call.respondText("post /jobs")
            }
        }
    }
}