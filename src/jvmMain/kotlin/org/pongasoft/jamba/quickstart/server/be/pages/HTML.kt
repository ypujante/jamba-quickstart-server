package org.pongasoft.jamba.quickstart.server.be.pages

import io.ktor.application.Application
import io.ktor.util.KtorExperimentalAPI
import org.pongasoft.jamba.quickstart.server.be.Beans

/**
 * Main api: installs all other apis
 */
@KtorExperimentalAPI
fun Application.HTML(beans: Beans) {
  static()
  indexHTML(beans.blankPluginMgr)
}
