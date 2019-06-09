package org.pongasoft.jamba.quickstart.server.be.services

import org.linkedin.util.lang.LangUtils
import org.pongasoft.jamba.quickstart.server.be.utils.syncOut
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.time.Clock
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

interface Reloadable {
  fun reload()
}

/**
 * Loads all the file in memory. Use reload to reload them from the filesystem.
 */
class BlankPluginCache(val blankPluginRoot: File,
                       val clock: Clock = Clock.systemDefaultZone(),
                       val UUIDGenerator : () -> UUID = { UUID.randomUUID() }) : ZipFileCreator, Reloadable {

  val MODULE = BlankPluginCache::class.java.name!!
  val log = org.slf4j.LoggerFactory.getLogger(MODULE)!!

  private var _files : Map<String, String> = emptyMap()
  private var _jambaGitHash = ""

  init { reload() }

  /**
   * Populates the cache */
  override fun reload() {
    log.info("Reloading caches from ${blankPluginRoot.canonicalPath}")
    _files = blankPluginRoot.walkTopDown()
        .filter { it.isFile }
        .associateBy({ it.relativeTo(blankPluginRoot).path}, { it.readText() })
    _jambaGitHash = computeGitHash(blankPluginRoot)
    log.info("Cache reloaded. count=${_files.size}, git=$_jambaGitHash")
  }

  /**
   * Processes each entry in the cache through the token replacement mechanism and invoke action
   * with the result */
  fun forEach(tokens: Map<String, String>, action: (String, String) -> Unit) {

    val newTokens = tokens.toMutableMap()

    val setToken = { key: String, value: String ->
      newTokens.getOrPut(key, {value})
    }

    val setBooleanToken = { key: String ->
      newTokens[key] = if(LangUtils.convertToBoolean(newTokens[key])) "ON" else "OFF"
    }

    val pluginName = setToken("name", "Plugin")
    setToken("jamba_git_hash", _jambaGitHash)

    val ns = tokens["namespace"]?.trim()

    if(ns == null || ns.isEmpty()) {
      newTokens["namespace_start"] = ""
      newTokens["namespace_end"] = ""
    } else {
      newTokens["namespace_start"] = ns.split("::").joinToString(separator = "\n") { "namespace $it {" }
      newTokens["namespace_end"] = ns.split("::").joinToString(separator = "\n") { "}" }
    }

    setToken("processor_uuid", generateUUID())
    setToken("controller_uuid", generateUUID())
    setToken("year", LocalDate.now(clock).year.toString())
    setToken("jamba_root_dir", "../../pongasoft/jamba")
    setToken("local_jamba", "#")
    setToken("remote_jamba", "")
    setBooleanToken("enable_vst2")
    setBooleanToken("enable_audio_unit")

    val t = newTokens.mapKeys { (k,_) -> "[-$k-]" }
    _files.forEach { (name, content) ->
      val processedName = name.replace("__Plugin__", pluginName)
      var processedContent = content
      for((tokenName, tokenValue) in t) {
        processedContent = processedContent.replace(tokenName, tokenValue)
      }
      action(processedName, processedContent)
    }
  }

  /**
   * Processes each entry in the cache through the token replacement mechanism and generate a zip file
   *
   * @param zipFile the location of the file that will be generated
   * @return the zip file provided as argument
   */
  override fun generateZipFile(zipFile: File, tokens: Map<String, String>) : File {

    FileSystems.newFileSystem(URI.create("jar:file:${zipFile.canonicalPath}"),
                              mapOf("create" to "true"),
                              null).use { zfs ->

      val root = zipFile.name.removeSuffix(".${zipFile.extension}")

      forEach(tokens) { entry, content ->
        val path = zfs.getPath(root, entry)
        if(path.parent != null)
          Files.createDirectories(path.parent)
        Files.copy(content.byteInputStream(), path)
      }
    }
    return zipFile
  }

  /**
   * Computes the git hash
   */
  private fun computeGitHash(rootDir: File): String {

    val hashResult =
        ProcessBuilder(listOf("git", "--no-pager", "log", "-1", "--pretty=format:%H"))
            .redirectErrorStream(true)
            .directory(rootDir)
            .start()
            .syncOut(timeout = 10, unit = TimeUnit.SECONDS)

    return if(hashResult.exitValue == 0) {
      val tagHashResult =
          ProcessBuilder(listOf("git", "describe", "--tags", hashResult.stdout))
              .redirectErrorStream(true)
              .directory(rootDir)
              .start()
              .syncOut(timeout = 10, unit = TimeUnit.SECONDS)
      if(tagHashResult.exitValue == 0)
        tagHashResult.stdout
      else
      {
        log.warn("Could not determine git tag hash: ${tagHashResult.stdout}")
        hashResult.stdout
      }
    } else {
      log.warn("Could not determine git hash: ${hashResult.stdout}")
      "unknown"
    }
  }

  /**
   * Generate a UUID as a C notation
   */
  private fun generateUUID() : String {
    val hex = UUIDGenerator().toString().replace("-", "")
    return "0x${hex.substring(0..7)}, 0x${hex.substring(8..15)}, 0x${hex.substring(16..23)}, 0x${hex.substring(24..31)}"
  }

}