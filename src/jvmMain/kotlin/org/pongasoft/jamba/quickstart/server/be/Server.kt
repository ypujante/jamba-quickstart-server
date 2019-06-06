package org.pongasoft.jamba.quickstart.server.be

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey
import org.pongasoft.jamba.quickstart.server.be.services.JobsMgr
import org.slf4j.event.Level

fun main(args: Array<String>) {
  embeddedServer(Netty,
                 environment = commandLineEnvironment(args))
      .start(wait = true)
}

// handle auto head? https://ktor.io/servers/features/autoheadresponse.html
// handle status pages https://ktor.io/servers/features/status-pages.html
// use webjars? https://ktor.io/servers/features/webjars.html

fun Application.initServer() {
  environment.log.info("Initializing Server...")

  // add date and server headers
  install(DefaultHeaders)

  // handle Json objects as return value
  install(ContentNegotiation) {
    register(ContentType.Application.Json, JacksonConverter())
  }

  // log calls
  install(CallLogging) {
    level = Level.INFO
  }

  val beans = Beans()

  intercept(ApplicationCallPipeline.Call) {
    call.beans = beans
  }
}