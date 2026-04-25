package com.openlinker.settings

import com.openlinker.model.CustomUrlRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenLinkerRuleFileTransferTest {
    private val json = Json

    @Test
    fun `exports rules with version and preserves order`() {
        val exportedText = OpenLinkerRuleFileTransfer.exportText(
            listOf(
                CustomUrlRule(name = "First", urlTemplate = "https://example.com/first", enabled = true),
                CustomUrlRule(
                    name = "Second",
                    urlTemplate = "file://${'$'}{FILE_PATH}",
                    enabled = false,
                    browserId = "browser-id",
                    browserName = "Chrome",
                ),
            ),
        )

        val root = json.parseToJsonElement(exportedText).jsonObject
        val rules = root.getValue("rules").jsonArray

        assertEquals("1", root.getValue("version").jsonPrimitive.content)
        assertEquals("First", rules[0].jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("Second", rules[1].jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("false", rules[1].jsonObject.getValue("enabled").jsonPrimitive.content)
        assertFalse("browserId" in rules[1].jsonObject)
        assertFalse("browserName" in rules[1].jsonObject)
    }

    @Test
    fun `imports valid rules and skips invalid ones`() {
        val importResult = OpenLinkerRuleFileTransfer.parseImportText(
            """
            {
              "version": 1,
              "rules": [
                { "name": " Docs ", "urlTemplate": " https://example.com/docs ", "enabled": true },
                { "name": "", "urlTemplate": "https://example.com/blank-name", "enabled": true },
                { "name": "Bad Enabled", "urlTemplate": "https://example.com/bad", "enabled": "yes" },
                { "name": "Default Enabled", "urlTemplate": "file://${'$'}{FILE_PATH}" }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/docs", enabled = true),
                CustomUrlRule(name = "Default Enabled", urlTemplate = "file://${'$'}{FILE_PATH}", enabled = true),
            ),
            importResult.rules,
        )
        assertEquals(2, importResult.skippedEntries.size)
        assertEquals("Rule name cannot be empty.", importResult.skippedEntries[0].reason)
        assertEquals("Enabled must be true or false.", importResult.skippedEntries[1].reason)
    }

    @Test
    fun `imports ignore browser fields from rule files`() {
        val importResult = OpenLinkerRuleFileTransfer.parseImportText(
            """
            {
              "version": 1,
              "rules": [
                {
                  "name": "Docs",
                  "urlTemplate": "https://example.com/docs",
                  "enabled": true,
                  "browserId": "browser-id",
                  "browserName": "Old Chrome"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                CustomUrlRule(
                    name = "Docs",
                    urlTemplate = "https://example.com/docs",
                    enabled = true,
                ),
            ),
            importResult.rules,
        )
    }

    @Test
    fun `rejects files without a rules list`() {
        val exception = assertFailsWith<OpenLinkerRuleFileTransfer.RuleFileException> {
            OpenLinkerRuleFileTransfer.parseImportText("""{ "version": 1 }""")
        }

        assertEquals("The selected file must contain a rules list.", exception.message)
    }

    @Test
    fun `rejects unsupported versions`() {
        val exception = assertFailsWith<OpenLinkerRuleFileTransfer.RuleFileException> {
            OpenLinkerRuleFileTransfer.parseImportText(
                """
                {
                  "version": 2,
                  "rules": []
                }
                """.trimIndent(),
            )
        }

        assertEquals("Only OpenLinker rule files with version 1 are supported.", exception.message)
    }

    @Test
    fun `exported text stays pretty printed json`() {
        val exportedText = OpenLinkerRuleFileTransfer.exportText(
            listOf(CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/docs", enabled = true)),
        )

        assertTrue(exportedText.contains('\n'))
        assertTrue(exportedText.contains("\"rules\": ["))
        assertTrue(exportedText.contains("\"name\": \"Docs\""))
    }
}
