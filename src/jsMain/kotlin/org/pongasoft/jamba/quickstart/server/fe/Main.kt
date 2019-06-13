package org.pongasoft.jamba.quickstart.server.fe

import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.xhr.FormData
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.createElement
import kotlin.js.Promise

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

/**
 * Helper to compute an audio manufacturer code from the plugin name
 */
fun computeAudioUnitManufacturerCode(pluginName: String?): String {
  if (pluginName == null || pluginName.isEmpty())
    return ""

  return pluginName.substring(0..3).padEnd(4, 'x').capitalize()
}

/**
 * Helper to compute a default namespace from plugin name and company
 */
fun computeNamespace(pluginName: String?, company: String?): String {
  if (pluginName == null || pluginName.isEmpty())
    return ""

  return if (company == null || company.isEmpty())
    "VST::$pluginName"
  else
    "$company::VST::$pluginName"
}

/**
 * Helper to compute a default project name from plugin name and company
 */
fun computeProjectName(pluginName: String?, company: String?): String {
  if (pluginName == null || pluginName.isEmpty())
    return ""

  return if (company == null || company.isEmpty())
    "$pluginName-plugin"
  else
    "$company-$pluginName-plugin"
}


/**
 * Used in promise rejection when detecting error (status code != 200)
 */
open class HTTPException(val status: Short, val errorMessage: String) : Exception("[$status] $errorMessage") {
  constructor(response: Response) : this(response.status, response.statusText)
}

/**
 * Submit form and handle json result (kotlin js allow handling "dynamic" value which are untyped/unchecked)
 */
fun submitForm(submitElt: HTMLInputElement,
               onSuccessJson: (dynamic) -> Unit,
               onFailure: (Throwable) -> Unit) {
  submitElt.form?.let { form ->
    window.fetch(form.action,
                 RequestInit(method = form.method,
                             body = FormData(form)))
        .then { response ->
          if (response.status == 200.toShort()) {
            response.json()
          } else {
            Promise.reject(HTTPException(response))
          }
        }
        .then { json: dynamic ->
          onSuccessJson(json)
        }
        .catch(onFailure)
  }
}

/**
 * Helper to create/initialize a jsObject see
 * https://stackoverflow.com/questions/28150124/javascript-anonymous-object-in-kotlin
 */
inline fun jsObject(init: dynamic.() -> Unit): dynamic {
    val o = js("{}")
    init(o)
    return o
}

/**
 * Checks for the job to be completed: loops `repeat` times waiting `delayInMs` between repeats.
 *
 * @param onResult is called once the job completes successfully with the URI to download the zip file
 * @param onRepeat is called every time this function runs (up to `repeat` times) with the repeat countdown
 * @param onFailure is called if there is a failure while computing the job or too many retries
 */
fun checkJob(jobURI: String,
             delayInMs: Int,
             repeat: Int,
             onResult: (resultURI: String) -> Unit,
             onRepeat: (repeat: Int) -> Unit,
             onFailure: (message: String) -> Unit) {
  if(repeat <= 0)
  {
    onFailure("No response within reasonable time")
    return
  }

  onRepeat(repeat)

  // async fetch of the job status
  window.fetch(jobURI,
               RequestInit(method="GET"))
      .then { response ->
        if (response.status == 200.toShort()) {
          response.json()
        } else {
          Promise.reject(HTTPException(response))
        }
      }
      .then { json: dynamic ->
        when(json?.completionStatus as String?) {
          "OK" -> onResult(json.resultURI as String)
          "ERROR" -> onFailure(json.error as String)
           else -> {
             // not completed => looping
             window.setTimeout(timeout = delayInMs,
                               handler = { checkJob(jobURI, delayInMs, repeat - 1, onResult, onRepeat, onFailure) })
           }
        }
      }
      .catch {
        onFailure(it.message ?: "Unknown error")
      }
}

/**
 * Encapsulates a notification section where messages can be added */
class Notification(id: String) {
  val element = document.getElementById(id)!!

  private fun addTextLine(message: String, status: String? = null) {
    val div = document.createElement("div")
    if(status != null)
      div.classList.add(status)
    div.appendChild(document.createTextNode(message))
    element.appendChild(div)
  }

  fun progress(message: String) {
    addTextLine(message)
  }

  fun success(message: String) {
    addTextLine(message, "success")
  }

  fun error(message: String) {
    addTextLine(message, "error")
  }
}

/**
 * This is a "trick" to force the browser to download a file: create an anchor element and click it. For security
 * reasons this would not work if the uri is not the same domain. */
fun downloadFile(uri: String) {
  (document.createElement("a") { this as HTMLAnchorElement
    href = uri
    target = "_blank"
  } as HTMLAnchorElement).click()
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

  val notification = Notification("notification")

  // defines what happens when the plugin name is entered/changed
  elements["name"]?.onChange {
    elements["audio_unit_manufacturer_code"]?.setComputedValue(computeAudioUnitManufacturerCode(value))
    elements["namespace"]?.setComputedValue(computeNamespace(value, elements["company"]?.value))
    elements["project_name"]?.setComputedValue(computeProjectName(value, elements["company"]?.value))
    elements["submit"]?.disabled = value.isEmpty()
  }

  // defines what happens when the company is entered/changed
  elements["company"]?.onChange {
    elements["namespace"]?.setComputedValue(computeNamespace(elements["name"]?.value, value))
    elements["project_name"]?.setComputedValue(computeProjectName(elements["name"]?.value, value))
    elements["company_url"]?.setComputedValue("https://www.$value.com")
    elements["company_email"]?.setComputedValue("support@$value.com")
  }

  // handle submitting the form
  elements["submit"]?.addListener("click") {
    submitForm(this,
               onSuccessJson = { json ->
                 checkJob(jobURI = json.uri, delayInMs = 1000, repeat = 5,
                          onResult = { resultURI ->
                            downloadFile(resultURI)
                            notification.success("Download complete")
                          },
                          onRepeat = { r ->
                            notification.progress("Generating zip file" + ".".repeat(5-r))
                          },
                          onFailure = {
                            notification.error("Error $it")
                          })
               },
               onFailure = {
                 notification.error("Error ${it.message}")
               }
    )
  }

  notification.progress("Fill out the name at least and click \"Generate\"")
}