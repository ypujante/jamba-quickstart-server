package org.pongasoft.jamba.quickstart.server.fe

import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.xhr.FormData
import org.w3c.fetch.RequestInit
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.createElement

/**
 * Adding a listener where the element is passed back in the closure as "this" for convenience */
fun HTMLInputElement.addListener(type: String, block: HTMLInputElement.(event: Event) -> Unit) {
  addEventListener(type, { event -> block(event) })
}

/**
 * Shortcut for change event */
fun HTMLInputElement.onChange(block: HTMLInputElement.(event: Event) -> Unit) {
  addListener("change", block)
}

/**
 * Add a __computedValue field to the element to store the value that was computed so that when it gets
 * recomputed it can be updated but ONLY in the event the user has not manually modified it
 */
fun HTMLInputElement.setComputedValue(computedValue: String) {
  val dynElt : dynamic = this
  if(value.isEmpty() || value == dynElt.__computedValue)
    value = computedValue
  dynElt.__computedValue = computedValue
}

fun computeAudioUnitManufacturerCode(pluginName: String?): String {
  if (pluginName == null || pluginName.isEmpty())
    return ""

  return pluginName.substring(0..3).padEnd(4, 'x').capitalize()
}

fun computeNamespace(pluginName: String?, company: String?): String {
  if (pluginName == null || pluginName.isEmpty())
    return ""

  return if (company == null || company.isEmpty())
    "VST::$pluginName"
  else
    "$company::VST::$pluginName"
}

fun computeProjectName(pluginName: String?, company: String?): String {
  if (pluginName == null || pluginName.isEmpty())
    return ""

  return if (company == null || company.isEmpty())
    "$pluginName-plugin"
  else
    "$company-$pluginName-plugin"
}

/**
 * Submit form and handle json result
 */
fun submitForm(submitElt: HTMLInputElement, handler: (dynamic) -> Unit) {
  submitElt.form?.let { form ->
    window.fetch(form.action,
                 RequestInit(method = form.method,
                             body = FormData(form)))
        .then { response ->
          if (response.status == 200.toShort()) {
            response.json()
          } else
            println("there was an error...${response.status}")
        }
        .then { json: dynamic ->
          handler(json)
        }
  }
}

fun checkJob(jobURI: String,
             delayInMs: Int,
             repeat: Int,
             onReady: (resultURI: String) -> Unit,
             onFailure: (message: String) -> Unit) {
  if(repeat <= 0)
  {
    onFailure("No response within reasonable time")
    return
  }

  window.fetch(jobURI,
               RequestInit(method="GET"))
      .then { response ->
        if (response.status == 200.toShort()) {
          response.json()
        } else
          println("there was an error...${response.status}")
      }
      .then { json: dynamic ->
        if(json.resultURI != null)
        {
          onReady(json.resultURI as String)
        }
        else
        {
          println("Job not completed yet... looping")
          window.setTimeout(timeout = delayInMs,
                            handler = { checkJob(jobURI, delayInMs, repeat - 1, onReady, onFailure) })
        }
      }
}

/**
 * Main function invoked by the HTML to initialize callbacks
 */
@JsName("init")
@Suppress("unused")
fun init() {
  println("Initializing callbacks")

  val elements = arrayOf("name",
                         "enable_vst2",
                         "enable_audio_unit",
                         "audio_unit_manufacturer_code",
                         "filename",
                         "filename",
                         "company",
                         "company_url",
                         "company_email",
                         "namespace",
                         "project_name",
                         "submit").associateBy({ it }) { id ->
    document.getElementById(id) as? HTMLInputElement
  }

  elements["name"]?.onChange {
    println("changed name value => $value")
    elements["audio_unit_manufacturer_code"]?.setComputedValue(computeAudioUnitManufacturerCode(value))
    elements["namespace"]?.setComputedValue(computeNamespace(value, elements["company"]?.value))
    elements["project_name"]?.setComputedValue(computeProjectName(value, elements["company"]?.value))
    elements["submit"]?.disabled = value.isEmpty()
  }

  elements["company"]?.onChange {
    println("changed company value => $value")
    elements["namespace"]?.setComputedValue(computeNamespace(elements["name"]?.value, value))
    elements["project_name"]?.setComputedValue(computeProjectName(elements["name"]?.value, value))
    elements["company_url"]?.setComputedValue("https://www.$value.com")
    elements["company_email"]?.setComputedValue("support@$value.com")
  }

  elements["submit"]?.addListener("click") {
    submitForm(this) { json ->
      println("success from request => ${json.uri}")
      checkJob(jobURI = json.uri, delayInMs = 1000, repeat = 5,
               onReady = { resultURI ->
                 println("Got it!!! $resultURI")
                 (document.createElement("a") { this as HTMLAnchorElement
                   href = resultURI
                   target = "_blank"
                 } as HTMLAnchorElement).click()
               },
               onFailure = {
                 println("there was error $it")
               })
    }
  }
}