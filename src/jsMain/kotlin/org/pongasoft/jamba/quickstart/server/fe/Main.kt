package org.pongasoft.jamba.quickstart.server.fe

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.xhr.FormData
import org.w3c.fetch.RequestInit
import kotlin.browser.document
import kotlin.browser.window

class OptionEntry(val name: String,
                  val elt: HTMLInputElement) {

  fun addEventListener(type: String, block: OptionEntry.(event: Event) -> Unit) {
    elt.addEventListener(type, { event -> block(event) })
  }

  fun onChange(block: OptionEntry.(event: Event) -> Unit) {
    addEventListener("change", block)
  }

  var value : String
    get() = elt.value
    set(s) { elt.value = s }

  private var _computedValue : String? = null

  fun setComputedValue(computedValue: String) {
    if(value.isEmpty() || value == _computedValue)
      value = computedValue
    _computedValue = computedValue
  }
}

fun computeAudioUnitManufacturerCode(pluginName: String?) : String {
  if(pluginName == null || pluginName.isEmpty())
    return ""

  return pluginName.substring(0..3).padEnd(4, 'x').capitalize()
}

fun computeNamespace(pluginName: String?, company: String?) : String {
  if(pluginName == null || pluginName.isEmpty())
    return ""

  return if(company == null || company.isEmpty())
    "VST::$pluginName"
  else
    "$company::VST::$pluginName"
}

fun computeProjectName(pluginName: String?, company: String?) : String {
  if(pluginName == null || pluginName.isEmpty())
    return ""

  return if(company == null || company.isEmpty())
    "$pluginName-plugin"
  else
    "$company-$pluginName-plugin"
}


@JsName("init")
@Suppress("unused")
fun init() {
  println("Initializing callbacks")

  val entries = arrayOf("name",
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
                        "submit").associateBy({it}) { id ->
    OptionEntry(name=id, elt=document.getElementById(id) as HTMLInputElement)
  }

  entries["name"]?.onChange {
    println("changed name value => $value")
    entries["audio_unit_manufacturer_code"]?.setComputedValue(computeAudioUnitManufacturerCode(value))
    entries["namespace"]?.setComputedValue(computeNamespace(value, entries["company"]?.value))
    entries["project_name"]?.setComputedValue(computeProjectName(value, entries["company"]?.value))
    entries["submit"]?.elt?.disabled = value.isEmpty()
  }

  entries["company"]?.onChange {
    println("changed company value => $value")
    entries["namespace"]?.setComputedValue(computeNamespace(entries["name"]?.value, value))
    entries["project_name"]?.setComputedValue(computeProjectName(entries["name"]?.value, value))
    entries["company_url"]?.setComputedValue("https://www.$value.com")
    entries["company_email"]?.setComputedValue("support@$value.com")
  }

  entries["submit"]?.addEventListener("click") {
    elt.form?.let { form ->
      println("Submitting query...")
      window.fetch(form.action,
                   RequestInit(method = "POST",
                               body = FormData(form)))
          .then { response ->
            if(response.status == 200.toShort())
            {
//              println("success from request => ${response.json()}")
              response.json()
            }
            else
              println("there was an error...${response.status}")
          }
          .then { json : dynamic ->
            println("success from request => ${json.uri}")
          }
//      val xhr = XMLHttpRequest()
//      xhr.open(method = "POST", url = form.action, async = true)
//      xhr.onreadystatechange = {
//        if(xhr.readyState > 3 && xhr.status == 200.toShort()) {
//          println("success from request => ${xhr.responseText}")
//        } else {
//          println("request failure ${xhr.status}")
//        }
//
//      }
//      xhr.send(FormData(form))
    }
  }
}