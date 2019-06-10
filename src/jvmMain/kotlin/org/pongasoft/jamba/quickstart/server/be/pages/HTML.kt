package org.pongasoft.jamba.quickstart.server.be.pages

import io.ktor.application.Application
import org.pongasoft.jamba.quickstart.server.be.Beans

/**
 * Main api: installs all other apis
 */
fun Application.HTML(beans: Beans) {
  indexHTML(beans.blankPluginMgr)
}
