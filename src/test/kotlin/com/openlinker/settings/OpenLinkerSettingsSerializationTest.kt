package com.openlinker.settings

import com.intellij.util.xmlb.XmlSerializer
import com.openlinker.model.CustomUrlRule
import org.jdom.output.XMLOutputter
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenLinkerSettingsSerializationTest {
    @Test
    fun `settings state serializes rules`() {
        val state = OpenLinkerSettingsService.SettingsState().apply {
            rules = mutableListOf(
                OpenLinkerSettingsService.RuleState(
                    CustomUrlRule(
                        name = "Docs",
                        urlTemplate = "https://example.com/${'$'}{PROJECT_NAME}",
                        enabled = true,
                    ),
                ),
            )
        }
        val element = XmlSerializer.serialize(state)

        val xml = XMLOutputter().outputString(element)
        assertTrue(xml.contains("Docs"))
        assertTrue(xml.contains("https://example.com/${'$'}{PROJECT_NAME}"))
    }
}
