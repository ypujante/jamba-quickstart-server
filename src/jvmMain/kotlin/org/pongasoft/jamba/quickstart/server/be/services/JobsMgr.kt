package org.pongasoft.jamba.quickstart.server.be.services

import java.io.File
import java.time.Clock

interface JobsMgr {

}

/**
 * Generate the zip file
 */
interface ZipFileCreator {
  fun generateZipFile(zipFile: File, tokens: Map<String, String>): File
}

/**
 * Manages job */
class JobsMgrImpl(private val clock: Clock,
                  private val blankPluginCache: BlankPluginCache) : JobsMgr {
}
