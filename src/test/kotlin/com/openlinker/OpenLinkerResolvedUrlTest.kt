package com.openlinker

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenLinkerResolvedUrlTest {
    @Test
    fun `rejects empty and protocol only addresses`() {
        assertFalse(OpenLinkerResolvedUrl.canOpen(""))
        assertFalse(OpenLinkerResolvedUrl.canOpen("https://"))
        assertFalse(OpenLinkerResolvedUrl.canOpen("file://"))
    }

    @Test
    fun `accepts normal web and file addresses`() {
        assertTrue(OpenLinkerResolvedUrl.canOpen("https://www.google.com/search?q=OpenLinker"))
        assertTrue(OpenLinkerResolvedUrl.canOpen("file:///tmp/example.txt"))
    }

    @Test
    fun `parses local file urls into paths`() {
        assertEquals(Paths.get("/tmp/example.txt"), OpenLinkerResolvedUrl.toLocalPath("file:///tmp/example.txt"))
        assertEquals(Paths.get("/tmp/hello world.txt"), OpenLinkerResolvedUrl.toLocalPath("file:///tmp/hello%20world.txt"))
    }

    @Test
    fun `returns null for non file urls`() {
        assertNull(OpenLinkerResolvedUrl.toLocalPath("https://www.google.com"))
    }
}
