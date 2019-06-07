package org.pongasoft.jamba.quickstart.server.be

import io.ktor.application.Application
import io.ktor.application.ApplicationStopped
import io.ktor.application.ApplicationStopping
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val server = embeddedServer(Netty,
                              environment = commandLineEnvironment(args))

  Runtime.getRuntime().addShutdownHook(Thread {
    val log = server.environment.log
    log.info("Shutting down")
    try {
      server.stop(1, 5, TimeUnit.SECONDS)
    } catch (th: Throwable) {
      log.warn("Error while shutting down (ignored)", th)
      exitProcess(1)
    }
  })

  server.start(wait = true)
  server.environment.log.info("Shutdown complete.")
  exitProcess(0)
}

// handle auto head? https://ktor.io/servers/features/autoheadresponse.html
// handle status pages https://ktor.io/servers/features/status-pages.html
// use webjars? https://ktor.io/servers/features/webjars.html

fun Application.initServer(beans: Beans = Beans.default) {
  val log = environment.log

  log.info("Initializing Server...")

  environment.monitor.subscribe(ApplicationStopped) {
    log.info("Stopping Server... ${beans}")
    beans.jobsMgr.destroy()
  }

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
}