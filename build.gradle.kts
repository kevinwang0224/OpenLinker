plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

fun escapeHtml(text: String): String = buildString {
    text.forEach { char ->
        append(
            when (char) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                else -> char
            },
        )
    }
}

fun markdownInlineToHtml(text: String): String {
    val escapedText = escapeHtml(text)
    return Regex("`([^`]+)`").replace(escapedText) { matchResult ->
        "<code>${matchResult.groupValues[1]}</code>"
    }
}

fun changelogSection(changelog: String, version: String): String {
    val lines = changelog.lines()
    val startIndex = lines.indexOfFirst { it.trim().startsWith("## [$version]") }
    if (startIndex < 0) {
        return ""
    }

    val sectionLines = lines.drop(startIndex + 1).takeWhile { !it.trim().startsWith("## ") }
    return sectionLines.joinToString("\n").trim()
}

fun changelogMarkdownToHtml(markdown: String): String {
    val html = StringBuilder()
    var listOpen = false

    fun closeList() {
        if (listOpen) {
            html.append("</ul>\n")
            listOpen = false
        }
    }

    markdown.lines().forEach { line ->
        val trimmedLine = line.trim()
        when {
            trimmedLine.isBlank() -> Unit
            trimmedLine.startsWith("### ") -> {
                closeList()
                html.append("<h3>")
                    .append(markdownInlineToHtml(trimmedLine.removePrefix("### ").trim()))
                    .append("</h3>\n")
            }
            trimmedLine.startsWith("- ") -> {
                if (!listOpen) {
                    html.append("<ul>\n")
                    listOpen = true
                }
                html.append("<li>")
                    .append(markdownInlineToHtml(trimmedLine.removePrefix("- ").trim()))
                    .append("</li>\n")
            }
            else -> {
                closeList()
                html.append("<p>")
                    .append(markdownInlineToHtml(trimmedLine))
                    .append("</p>\n")
            }
        }
    }

    closeList()
    return html.toString().trim()
}

group = "com.openlinker"
version = "0.1.3"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    projectName = project.name

    pluginConfiguration {
        id = "com.openlinker"
        name = "OpenLinker"
        version = project.version.toString()
        description = "Open context-aware web or file links from customizable URL templates."
        changeNotes = providers.fileContents(layout.projectDirectory.file("CHANGELOG.md")).asText.map {
            changelogMarkdownToHtml(changelogSection(it, project.version.toString()))
        }

        ideaVersion {
            sinceBuild = "231"
            untilBuild = provider { null }
        }

        vendor {
            name = "OpenLinker"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
