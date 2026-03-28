package com.openlinker.settings

import com.openlinker.model.CustomUrlRule
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenLinkerSettingsServiceTest {
    @Test
    fun `starts with the default google rule`() {
        val service = OpenLinkerSettingsService()

        assertEquals(
            listOf(
                CustomUrlRule(
                    name = "Google",
                    urlTemplate = "https://www.google.com/search?q=${'$'}{PROJECT_NAME}",
                    enabled = true,
                ),
                CustomUrlRule(
                    name = "File",
                    urlTemplate = "file://${'$'}{FILE_PATH}",
                    enabled = true,
                ),
            ),
            service.getRules(),
        )
    }

    @Test
    fun `stores trimmed rules and keeps only enabled ones`() {
        val service = OpenLinkerSettingsService()
        service.setRules(
            listOf(
                CustomUrlRule(
                    name = " Docs ",
                    urlTemplate = " https://example.com/${'$'}{PROJECT_NAME} ",
                    enabled = true,
                ),
                CustomUrlRule(
                    name = " Disabled ",
                    urlTemplate = " https://example.com/disabled ",
                    enabled = false,
                ),
            ),
        )

        assertEquals(
            listOf(
                CustomUrlRule(
                    name = "Docs",
                    urlTemplate = "https://example.com/${'$'}{PROJECT_NAME}",
                    enabled = true,
                ),
                CustomUrlRule(
                    name = "Disabled",
                    urlTemplate = "https://example.com/disabled",
                    enabled = false,
                ),
            ),
            service.getRules(),
        )
        assertEquals(
            listOf(
                CustomUrlRule(
                    name = "Docs",
                    urlTemplate = "https://example.com/${'$'}{PROJECT_NAME}",
                    enabled = true,
                ),
            ),
            service.getEnabledRules(),
        )
    }
}
