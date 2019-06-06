package org.pongasoft.jamba.quickstart.server.be.services

import org.pongasoft.jamba.quickstart.server.be.utils.child
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.time.Clock
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlankPluginCacheTest
{
  val root = File("src/jvmTest/resources/plugin")
  val clock = Clock.fixed(Clock.systemDefaultZone().instant(),
                          Clock.systemDefaultZone().zone)
  val UUIDString = "c935d2e3-3605-4cae-ae29-d55f76c53f3d"
  val UUIDGenerator = { UUID.fromString(UUIDString) }

  @Test
  fun testCache() {
    val cache = BlankPluginCache(root, clock, UUIDGenerator)

    val tokens = mapOf("name" to "TestPlug",
                       "token1" to "flower",
                       "token2" to "blue",
                       "token3" to "menu",
                       "namespace" to "ns1::ns2")

    val expectedEntries =
        mutableMapOf("MainTestPlug.h" to """
                       |// this is a flower and blue
                       |// this is a menu and flower""".trimMargin(),
                     "dir1/subfile1.cpp" to "// this is a flower and [-tokenX-]",
                     "file1.h" to "// no tokens",
                     "cid.h" to """
                       |// copyright ${LocalDate.now(clock).year}
                       |namespace ns1 {
                       |namespace ns2 {
                       |static const ::Steinberg::FUID TestPlugProcessorUID(0xc935d2e3, 0x36054cae, 0xae29d55f, 0x76c53f3d);
                       |}
                       |}
                       """.trimMargin()
              )

    cache.forEach(tokens) { entry, content ->
      assertTrue(expectedEntries.containsKey(entry))
      assertEquals(expectedEntries.remove(entry), content)
    }

    assertEquals(0, expectedEntries.size)
  }

  @Test
  fun testZipCreator() {
    val cache = BlankPluginCache(root, clock, UUIDGenerator)

    val tokens = mapOf("name" to "TestPlug",
                       "token1" to "flower",
                       "token2" to "blue",
                       "token3" to "menu",
                       "namespace" to "ns1::ns2")

    val dir = Files.createTempDirectory(null)

    val zipFile = dir.toFile().child("foo.zip")
    zipFile.deleteOnExit()

    cache.generateZipFile(zipFile, tokens)

    val expectedEntries =
        mutableMapOf("/" to null,
                     "/foo/" to null,
                     "/foo/dir1/" to null,
                     "/foo/MainTestPlug.h" to """
                       |// this is a flower and blue
                       |// this is a menu and flower""".trimMargin(),
                     "/foo/dir1/subfile1.cpp" to "// this is a flower and [-tokenX-]",
                     "/foo/file1.h" to "// no tokens",
                     "/foo/cid.h" to """
                       |// copyright ${LocalDate.now(clock).year}
                       |namespace ns1 {
                       |namespace ns2 {
                       |static const ::Steinberg::FUID TestPlugProcessorUID(0xc935d2e3, 0x36054cae, 0xae29d55f, 0x76c53f3d);
                       |}
                       |}
                       """.trimMargin())

    // check content of zip file
    FileSystems.newFileSystem(URI.create("jar:file:${zipFile.canonicalPath}"),
                              emptyMap<String, String>()).use { zfs ->

      for(p in Files.walk(zfs.getPath("/"))) {
        assertTrue(expectedEntries.containsKey(p.toString()), "$p not expected")
        val expectedContent = expectedEntries.remove(p.toString())
        if(expectedContent == null)
          assertTrue(Files.isDirectory(p)) // directory
        else
          assertEquals(expectedContent, String(Files.readAllBytes(p))) // expected content
      }
    }

    assertEquals(0, expectedEntries.size)
  }
}