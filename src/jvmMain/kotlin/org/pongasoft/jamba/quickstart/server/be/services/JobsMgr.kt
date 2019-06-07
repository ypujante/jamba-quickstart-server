package org.pongasoft.jamba.quickstart.server.be.services

import org.linkedin.util.lifecycle.Destroyable
import java.io.File
import java.time.Clock
import kotlin.concurrent.withLock

interface JobsMgr : Destroyable {

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

  /**
   * This method destroys the entity, cleaning up any resource that needs to
   * be cleaned up, like closing files, database connection..  */
  override fun destroy() {
//    log.info("JobMgr - destroy")
//    _jobQueue.shutdown()
//    _lock.withLock {
//      _jobs.values.forEach { cleanup(it.jobRun) }
//      _jobs.clear()
//    }
  }

}
