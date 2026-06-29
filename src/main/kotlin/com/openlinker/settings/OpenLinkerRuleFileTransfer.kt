package com.openlinker.settings

import com.openlinker.model.CustomUrlRule
import com.openlinker.model.ProjectUrlOverride
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
                                put(
                                    "projectOverrides",
                                    buildJsonArray {
                                        rule.projectOverrides.forEach { projectOverride ->
                                            add(
                                                buildJsonObject {
                                                    put("projectName", projectOverride.projectName)
                                                    put("urlTemplate", projectOverride.urlTemplate)
                                                },
                                            )
                                        }
                                    },
                                )
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
                is ParsedRule.Valid -> {
                    importedRules.add(parseResult.rule)
                    skippedEntries.addAll(parseResult.issues)
                }
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

        val projectOverrideResult = parseProjectOverrides(entryIndex, ruleObject["projectOverrides"])
        return ParsedRule.Valid(
            CustomUrlRule(
                name = name,
                urlTemplate = urlTemplate,
                enabled = enabled,
                projectOverrides = projectOverrideResult.projectOverrides,
            ).normalized(),
            projectOverrideResult.issues,
        )
    }

    private fun parseProjectOverrides(entryIndex: Int, entry: JsonElement?): ProjectOverrideParseResult {
        if (entry == null) {
            return ProjectOverrideParseResult(emptyList(), emptyList())
        }

        val overrideEntries = entry as? JsonArray
            ?: return ProjectOverrideParseResult(
                emptyList(),
                listOf(ImportIssue(entryIndex, "Project overrides must be a list.")),
            )

        val projectOverrides = mutableListOf<ProjectUrlOverride>()
        val issues = mutableListOf<ImportIssue>()
        overrideEntries.forEach { overrideEntry ->
            val overrideObject = overrideEntry as? JsonObject
            if (overrideObject == null) {
                issues.add(ImportIssue(entryIndex, "Project override entry must be an object."))
                return@forEach
            }

            val projectName = overrideObject.readString("projectName")
            if (projectName == null) {
                issues.add(ImportIssue(entryIndex, "Project override project name must be a string."))
                return@forEach
            }

            val urlTemplate = overrideObject.readString("urlTemplate")
            if (urlTemplate == null) {
                issues.add(ImportIssue(entryIndex, "Project override URL template must be a string."))
                return@forEach
            }

            when {
                projectName.isBlank() -> issues.add(
                    ImportIssue(entryIndex, "Project override project name cannot be empty."),
                )
                urlTemplate.isBlank() -> issues.add(
                    ImportIssue(entryIndex, "Project override URL template cannot be empty."),
                )
                else -> projectOverrides.add(
                    ProjectUrlOverride(
                        projectName = projectName,
                        urlTemplate = urlTemplate,
                    ),
                )
            }
        }

        return ProjectOverrideParseResult(projectOverrides, issues)
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
        data class Valid(
            val rule: CustomUrlRule,
            val issues: List<ImportIssue> = emptyList(),
        ) : ParsedRule

        data class Invalid(val issue: ImportIssue) : ParsedRule {
            constructor(entryIndex: Int, reason: String) : this(ImportIssue(entryIndex, reason))
        }
    }

    private data class ProjectOverrideParseResult(
        val projectOverrides: List<ProjectUrlOverride>,
        val issues: List<ImportIssue>,
    )
}
