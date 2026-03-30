package com.openlinker.settings

import com.openlinker.model.CustomUrlRule
import com.openlinker.model.normalized
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

object OpenLinkerRuleFileTransfer {
    const val DEFAULT_FILE_NAME = "openlinker-rules.json"

    private const val FILE_VERSION = 1
    private val json = Json {
        prettyPrint = true
    }

    data class ImportIssue(
        val entryIndex: Int,
        val reason: String,
    )

    data class ImportResult(
        val rules: List<CustomUrlRule>,
        val skippedEntries: List<ImportIssue>,
    )

    class RuleFileException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun exportRules(path: Path, rules: List<CustomUrlRule>) {
        path.writeText(exportText(rules) + "\n")
    }

    fun exportText(rules: List<CustomUrlRule>): String {
        val normalizedRules = rules.normalized()
        val root = buildJsonObject {
            put("version", FILE_VERSION)
            put(
                "rules",
                buildJsonArray {
                    normalizedRules.forEach { rule ->
                        add(
                            buildJsonObject {
                                put("name", rule.name)
                                put("urlTemplate", rule.urlTemplate)
                                put("enabled", rule.enabled)
                            },
                        )
                    }
                },
            )
        }

        return json.encodeToString(JsonElement.serializer(), root)
    }

    fun importRules(path: Path): ImportResult = parseImportText(path.readText())

    fun parseImportText(content: String): ImportResult {
        val root = try {
            json.parseToJsonElement(content) as? JsonObject
                ?: throw RuleFileException("The selected file must contain a JSON object.")
        } catch (exception: SerializationException) {
            throw RuleFileException("The selected file is not valid JSON.", exception)
        }

        validateVersion(root)

        val ruleEntries = root["rules"] as? JsonArray
            ?: throw RuleFileException("The selected file must contain a rules list.")

        val importedRules = mutableListOf<CustomUrlRule>()
        val skippedEntries = mutableListOf<ImportIssue>()

        ruleEntries.forEachIndexed { index, entry ->
            when (val parseResult = parseRuleEntry(index + 1, entry)) {
                is ParsedRule.Valid -> importedRules.add(parseResult.rule)
                is ParsedRule.Invalid -> skippedEntries.add(parseResult.issue)
            }
        }

        return ImportResult(
            rules = importedRules,
            skippedEntries = skippedEntries,
        )
    }

    private fun validateVersion(root: JsonObject) {
        val versionElement = root["version"] ?: return
        val version = (versionElement as? JsonPrimitive)?.intOrNull
        if (version != FILE_VERSION) {
            throw RuleFileException("Only OpenLinker rule files with version 1 are supported.")
        }
    }

    private fun parseRuleEntry(entryIndex: Int, entry: JsonElement): ParsedRule {
        val ruleObject = entry as? JsonObject
            ?: return ParsedRule.Invalid(entryIndex, "Entry must be an object.")

        val name = ruleObject.readString("name")
            ?: return ParsedRule.Invalid(entryIndex, "Rule name must be a string.")
        val urlTemplate = ruleObject.readString("urlTemplate")
            ?: return ParsedRule.Invalid(entryIndex, "URL template must be a string.")
        val enabled = ruleObject.readBoolean("enabled")
            ?: return ParsedRule.Invalid(entryIndex, "Enabled must be true or false.")

        if (name.isBlank()) {
            return ParsedRule.Invalid(entryIndex, "Rule name cannot be empty.")
        }
        if (urlTemplate.isBlank()) {
            return ParsedRule.Invalid(entryIndex, "URL template cannot be empty.")
        }

        return ParsedRule.Valid(
            CustomUrlRule(
                name = name,
                urlTemplate = urlTemplate,
                enabled = enabled,
            ).normalized(),
        )
    }

    private fun JsonObject.readString(fieldName: String): String? {
        val value = this[fieldName] as? JsonPrimitive ?: return null
        return if (value.isString) value.content.trim() else null
    }

    private fun JsonObject.readBoolean(fieldName: String): Boolean? {
        val value = this[fieldName] ?: return true
        return (value as? JsonPrimitive)?.booleanOrNull
    }

    private sealed interface ParsedRule {
        data class Valid(val rule: CustomUrlRule) : ParsedRule

        data class Invalid(val issue: ImportIssue) : ParsedRule {
            constructor(entryIndex: Int, reason: String) : this(ImportIssue(entryIndex, reason))
        }
    }
}
