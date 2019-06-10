package org.pongasoft.jamba.quickstart.server.be

import io.ktor.application.Application
import io.ktor.application.ApplicationStopped
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.Routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import org.linkedin.util.lifecycle.Destroyable
import org.pongasoft.jamba.quickstart.server.be.api.api
import org.pongasoft.jamba.quickstart.server.be.pages.HTML
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

// check https://github.com/JetBrains/kotlinconf-app/blob/master/backend/src/org/jetbrains/kotlinconf/backend/Main.kt
// for inspiration and details

/**
 * Main entry point (check application.conf).
 */
@KtorExperimentalAPI
fun Application.initServer() {
  // extract username and password from config
  val username = environment.config.propertyOrNull("api.admin.username")?.getString()
  val password = environment.config.propertyOrNull("api.admin.password")?.getString()

  initServer(Beans(adminUserName = username,
                   adminPassword = password))
}

/**
 * Initializes the server
 */
fun Application.initServer(beans: Beans) {
  val log = environment.log

  log.info("Initializing Server...")

  // register an event to stop the server when the application shuts down
  environment.monitor.subscribe(ApplicationStopped) {
    log.info("Stopping Server...")

    // if the jobs mgr is destroyable then destroy it
    (beans.jobsMgr as? Destroyable)?.destroy()
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

  // Configure routing
  install(Routing) {
    api(beans)
    HTML(beans)
  }
}
