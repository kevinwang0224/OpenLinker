package com.openlinker.settings

import com.intellij.util.xmlb.XmlSerializer
import com.openlinker.model.CustomUrlRule
import com.openlinker.model.ProjectUrlOverride
import org.jdom.output.XMLOutputter
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenLinkerSettingsSerializationTest {
    @Test
    fun `settings state serializes browser preferences and rules`() {
        val state = OpenLinkerSettingsService.SettingsState().apply {
            globalBrowserId = "global-browser-id"
            globalBrowserName = "Chrome"
            rules = mutableListOf(
                OpenLinkerSettingsService.RuleState(
                    CustomUrlRule(
                        name = "Docs",
                        urlTemplate = "https://example.com/${'$'}{PROJECT_NAME}",
                        enabled = true,
                        browserId = "rule-browser-id",
                        browserName = "Firefox",
                        projectOverrides = listOf(
                            ProjectUrlOverride(
                                projectName = "OpenLinker",
                                urlTemplate = "https://project.example.com/${'$'}{PROJECT_NAME}",
                            ),
                        ),
                    ),
                ),
            )
        }
        val element = XmlSerializer.serialize(state)

        val xml = XMLOutputter().outputString(element)
        assertTrue(xml.contains("global-browser-id"))
        assertTrue(xml.contains("Chrome"))
        assertTrue(xml.contains("Docs"))
        assertTrue(xml.contains("https://example.com/${'$'}{PROJECT_NAME}"))
        assertTrue(xml.contains("rule-browser-id"))
        assertTrue(xml.contains("Firefox"))
        assertTrue(xml.contains("OpenLinker"))
        assertTrue(xml.contains("https://project.example.com/${'$'}{PROJECT_NAME}"))
    }
}
