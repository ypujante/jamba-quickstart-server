package org.pongasoft.jamba.quickstart.server.be.pages

import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.html.BODY
import kotlinx.html.HEAD
import kotlinx.html.link
import kotlinx.html.script
import kotlinx.html.unsafe
import java.io.File

/**
 * Function to add the javascript */
fun BODY.scripts(staticPath: String) {
  script(src="$staticPath/require.min.js") {}
  script {
    unsafe {
      +"require.config({baseUrl: '$staticPath'});\n"
      +"require(['$staticPath/jamba-quickstart-server.js'], function(js) {js.org.pongasoft.jamba.quickstart.server.fe.init(); });\n"
    }
  }
}

/**
 * Function to add css
 */
fun HEAD.css(staticPath: String) {
  link(rel = "stylesheet", href="$staticPath/main.css")
}

/**
 * Adding extension function to be able to change the static path via config */
@KtorExperimentalAPI
val ApplicationConfig.staticPath : String get() { return propertyOrNull("api.static.path")?.getString() ?: "/static" }

/**
 * Mounts static path to be served (javascript) */
@KtorExperimentalAPI
fun Application.static() {
  // The property can be added in the `application.conf` file or more likely from the command line
  // `-P:api.static.webdir=pathToWebdir`. See `build.gradle` for an example. */
  val webDir : File? = environment.config.propertyOrNull("api.static.webdir")?.getString()?.let { File(it) }

  if(webDir != null && webDir.isDirectory)
  {
    environment.log.info("Installing ${environment.config.staticPath} to serve files under ${webDir.canonicalPath}")

    routing {
      static(environment.config.staticPath) {
        files(webDir)
      }
    }
  }

}