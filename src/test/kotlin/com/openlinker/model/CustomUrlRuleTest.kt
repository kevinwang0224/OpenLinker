package com.openlinker.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CustomUrlRuleTest {
    @Test
    fun `uses project override when project name matches`() {
        val rule = CustomUrlRule(
            name = "Docs",
            urlTemplate = "https://example.com/global/${'$'}{PROJECT_NAME}",
            projectOverrides = listOf(
                ProjectUrlOverride(
                    projectName = "OpenLinker",
                    urlTemplate = "https://example.com/openlinker/${'$'}{PROJECT_NAME}",
                ),
            ),
        )

        assertEquals(
            "https://example.com/openlinker/${'$'}{PROJECT_NAME}",
            rule.urlTemplateForProject("OpenLinker"),
        )
        assertEquals(
            "https://example.com/global/${'$'}{PROJECT_NAME}",
            rule.urlTemplateForProject("OtherProject"),
        )
    }

    @Test
    fun `normalization trims project overrides and keeps the last duplicate project`() {
        val rule = CustomUrlRule(
            name = " Docs ",
            urlTemplate = " https://example.com/global ",
            projectOverrides = listOf(
                ProjectUrlOverride(
                    projectName = " OpenLinker ",
                    urlTemplate = " https://example.com/old ",
                ),
                ProjectUrlOverride(
                    projectName = "",
                    urlTemplate = "https://example.com/blank-project",
                ),
                ProjectUrlOverride(
                    projectName = "OpenLinker",
                    urlTemplate = " https://example.com/new ",
                ),
            ),
        ).normalized()

        assertEquals(
            listOf(
                ProjectUrlOverride(
                    projectName = "OpenLinker",
                    urlTemplate = "https://example.com/new",
                ),
            ),
            rule.projectOverrides,
        )
    }
}
