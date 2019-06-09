package org.pongasoft.jamba.quickstart.server.be.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.basic
import io.ktor.html.respondHtml
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.title
import kotlinx.html.ul
import org.pongasoft.jamba.quickstart.server.be.services.BlankPluginMgr
import org.pongasoft.jamba.quickstart.server.be.services.Reloadable

/**
 * Defines the api to admin features
 */
fun Application.adminAPI(adminUserName: String, adminPassword: String, blankPluginMgr: BlankPluginMgr) {
  val log = environment.log

  // we protect the section behind authentication
  authentication {
    log.info("Installing basic auth for /admin")
    basic(name = "adminAuth") {
      realm = "Jamba Quickstart - Admin"
      validate { credentials ->
        if(credentials.name == adminUserName && credentials.password == adminPassword)
          UserIdPrincipal(credentials.name)
        else
          null
      }
    }
  }

  routing {
    route("$API_URL/admin") {
      authenticate("adminAuth") {

        /**
         * GET /admin/status => returns ok
         */
        get("status") {
          call.respond(mapOf("status" to "ok"))
        }

        /**
         * GET /admin/reload
         */
        get("reload") {
          if(blankPluginMgr is Reloadable)
            blankPluginMgr.reload()
          call.respond(mapOf("fileCount" to blankPluginMgr.fileCount,
                             "hash" to blankPluginMgr.jambaGitHash))
        }

        /**
         * GET /admin => returns html with links to various admin features
         */
        get {
          call.respondHtml {
            head {
              title("Jamba Quickstart - Admin")
            }

            body {
              h1 { +"Jamba Quickstart - Admin" }
              +"Blank Plugin Info: fileCount=${blankPluginMgr.fileCount}, hash=${blankPluginMgr.jambaGitHash}"
              ul {
                li {
                  a(href = "$API_URL/admin/status") { +"Check status" }
                }
                li {
                  a(href = "$API_URL/admin/reload") { +"Reload Cache" }
                }
              }
            }
          }
        }
      }
    }
  }
}