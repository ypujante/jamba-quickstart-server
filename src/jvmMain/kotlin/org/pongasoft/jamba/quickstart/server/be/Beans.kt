package org.pongasoft.jamba.quickstart.server.be

import org.pongasoft.jamba.quickstart.server.be.services.BlankPluginCache
import org.pongasoft.jamba.quickstart.server.be.services.BlankPluginMgr
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
                 val blankPluginMgr: BlankPluginMgr =
                     BlankPluginCache(blankPluginRoot = blankPluginRootDir,
                                      clock = clock),
                 val jobsMgr: JobsMgr = JobsMgrImpl(clock, blankPluginMgr),
                 val adminUserName: String? = null,
                 val adminPassword: String? = null)
