package org.pongasoft.jamba.quickstart.server.be

import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
  embeddedServer(Netty,
                 environment = commandLineEnvironment(args))
      .start(wait = true)
}

