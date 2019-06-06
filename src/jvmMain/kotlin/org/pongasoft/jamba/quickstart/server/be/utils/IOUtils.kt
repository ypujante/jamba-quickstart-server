package org.pongasoft.jamba.quickstart.server.be.utils

import java.io.File

/**
 * Simple extension function to simplify the creation of multiple children by chaining (vs nested constructors)
 *
 * @return the child
 */
fun File.child(childName: String): File = File(this, childName)
