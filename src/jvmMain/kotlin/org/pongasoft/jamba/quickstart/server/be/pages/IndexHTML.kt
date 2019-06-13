package org.pongasoft.jamba.quickstart.server.be.pages

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.TBODY
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.meta
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.title
import kotlinx.html.tr
import org.pongasoft.jamba.quickstart.server.be.api.API_URL
import org.pongasoft.jamba.quickstart.server.be.services.BlankPluginMgr

data class OptionEntry(val name: String,
                       val label: String,
                       val type: InputType = InputType.text,
                       val defaultValue: String = "",
                       val desc: String = "")

fun TBODY.optionEntry(entry: OptionEntry): Unit = tr {
  td("name") { label { htmlFor = entry.name; +entry.label } }
  td("control") {
    input(type = entry.type, name = entry.name) {
      id = entry.name
      value = entry.defaultValue
      if(entry.type == InputType.checkBox)
        checked = true
      +entry.defaultValue
    }
  }
  td("desc") { +entry.desc }
}

@KtorExperimentalAPI
fun Application.indexHTML(blankPluginMgr: BlankPluginMgr) {

  val entries =
      arrayOf(OptionEntry(name = "name",
                          label = "Plugin Name",
                          desc = "Must be a valid C++ class name"),
              OptionEntry(name = "enable_vst2",
                          type = InputType.checkBox,
                          label = "Enable VST2",
                          desc = "Makes the plugin compatible with both VST2 and VST3"),
              OptionEntry(name = "enable_audio_unit",
                          type = InputType.checkBox,
                          label = "Enable Audio Unit",
                          desc = "Generates an (additional) Audio Unit compatible plugin"),
              OptionEntry(name = "audio_unit_manufacturer_code",
                          label = "Audio Unit Manufacturer",
                          desc = "Must be 4 characters with (at least) one capital letter"),
              OptionEntry(name = "filename",
                          label = "Filename",
                          desc = "The name used for the plugin file (building the plugin will generate <Filename>.VST3)"),
              OptionEntry(name = "company",
                          label = "Company",
                          desc = "Name of the company (your name if not company)"),
              OptionEntry(name = "company_url",
                          label = "Company URL",
                          desc = "A URL for the company (a link to reach you if not a company)"),
              OptionEntry(name = "company_email",
                          label = "Company Email",
                          desc = "An email address for the company (your email of not a company)"),
              OptionEntry(name = "namespace",
                          label = "C++ namespace",
                          desc = "Although recommended, you can leave blank if you do not want to use a namespace"),
              OptionEntry(name = "project_name",
                          label = "Project name",
                          desc = "Name of the project itself (which will be the name of the zip file generated)")
      )

  routing {
    get("/index.html") {
      call.respondHtml {
        head {
          meta(charset = "UTF-8")
          title("Jamba - Quickstart")
          css(environment.config.staticPath)
        }

        body {
          h1 { +"Jamba - Quick Start" }
          form(method = FormMethod.post, action = "$API_URL/jobs") {
            table {
              tbody {
                entries.forEach { optionEntry(it) }
                tr {
                  td {
                    colSpan = "3"
                    input(type = InputType.button) {
                      id = "submit"
                      value = "Generate blank plugin (Jamba ${blankPluginMgr.jambaGitHash})"
                      disabled = true
                    }
                  }
                }
              }
            }
          }
          div {id = "notification"}
          scripts(environment.config.staticPath)
        }
      }

    }
  }
}