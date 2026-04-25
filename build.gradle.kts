plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij.platform") version "2.11.0"
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
