package org.pongasoft.jamba.quickstart.server.be

import io.ktor.application.ApplicationCall
import io.ktor.util.AttributeKey
import org.pongasoft.jamba.quickstart.server.be.services.BlankPluginCache
import org.pongasoft.jamba.quickstart.server.be.services.JobsMgr
import org.pongasoft.jamba.quickstart.server.be.services.JobsMgrImpl
import java.io.File
import java.time.Clock

/**
 * Contains the beans that will be available via <code>call.beans</code>
 */
data class Beans(val clock: Clock = Clock.systemUTC(),
                 val blankPluginRootDir: File =
                     File(System.getProperty("org.pongasoft.jamba.quickstart.server.blankPluginRootDir",
                                             "/Volumes/Development/github/org.pongasoft/jamba/blank-plugin")),
                 val blankPluginCache: BlankPluginCache =
                     BlankPluginCache(blankPluginRoot = blankPluginRootDir,
                                      clock = clock),
                 val jobsMgr: JobsMgr = JobsMgrImpl(clock, blankPluginCache))

private val beansKey = AttributeKey<Beans>("Beans")

/**
 * Exposes beans via <code>call.beans</code>
 */
var ApplicationCall.beans: Beans
  get() { return attributes[beansKey] }
  set(beans) { attributes.put(beansKey, beans) }
