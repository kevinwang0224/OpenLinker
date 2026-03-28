package com.openlinker.url

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenLinkerUrlTemplateResolverTest {
    @Test
    fun `replaces all supported variables`() {
        val context = OpenLinkerContext(
            projectName = "OpenLinker",
            moduleName = "plugin",
            fileName = "OpenCustomUrlAction.kt",
            filePath = "/tmp/OpenCustomUrlAction.kt",
        )

        val result = OpenLinkerUrlTemplateResolver.resolve(
            "https://example.com?project=${'$'}{PROJECT_NAME}&module=${'$'}{MODULE_NAME}&file=${'$'}{FILE_NAME}&path=${'$'}{FILE_PATH}",
            context,
        )

        assertEquals(
            "https://example.com?project=OpenLinker&module=plugin&file=OpenCustomUrlAction.kt&path=/tmp/OpenCustomUrlAction.kt",
            result,
        )
    }

    @Test
    fun `replaces missing values with empty strings`() {
        val result = OpenLinkerUrlTemplateResolver.resolve(
            "https://example.com/${'$'}{PROJECT_NAME}/${'$'}{MODULE_NAME}/${'$'}{FILE_NAME}",
            OpenLinkerContext(projectName = "OpenLinker"),
        )

        assertEquals("https://example.com/OpenLinker//", result)
    }
}
