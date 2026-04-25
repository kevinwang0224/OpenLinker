package com.openlinker.browser

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.browsers.WebBrowser
import com.intellij.ide.browsers.WebBrowserManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.openlinker.OpenLinkerConstants
import com.openlinker.model.OpenLinkerBrowserPreference
import com.openlinker.model.normalized

object OpenLinkerBrowsers {
    fun activeBrowsers(): List<WebBrowser> = withBrowserManager { it.activeBrowsers }.orEmpty()

    fun findBrowserById(browserId: String): WebBrowser? {
        val normalizedBrowserId = browserId.trim()
        if (normalizedBrowserId.isBlank()) {
            return null
        }

        return withBrowserManager { it.findBrowserById(normalizedBrowserId) }
    }

    fun preferenceFor(browser: WebBrowser?): OpenLinkerBrowserPreference {
        return if (browser == null) {
            OpenLinkerBrowserPreference()
        } else {
            OpenLinkerBrowserPreference(
                browserId = browser.id.toString(),
                browserName = browser.name,
            )
        }
    }

    fun effectivePreference(
        rulePreference: OpenLinkerBrowserPreference,
        globalPreference: OpenLinkerBrowserPreference,
    ): OpenLinkerBrowserPreference {
        val normalizedRulePreference = rulePreference.normalized()
        return if (normalizedRulePreference.isDefault) {
            globalPreference.normalized()
        } else {
            normalizedRulePreference
        }
    }

    fun displayName(preference: OpenLinkerBrowserPreference, defaultLabel: String): String {
        val normalizedPreference = preference.normalized()
        if (normalizedPreference.isDefault) {
            return defaultLabel
        }

        val browser = findBrowserById(normalizedPreference.browserId)
        return browser?.name ?: "Missing: ${fallbackName(normalizedPreference)}"
    }

    fun browse(project: Project?, url: String, preference: OpenLinkerBrowserPreference) {
        val normalizedPreference = preference.normalized()
        val browser = if (normalizedPreference.isDefault) {
            null
        } else {
            findBrowserById(normalizedPreference.browserId)
        }

        if (!normalizedPreference.isDefault && browser == null) {
            Messages.showWarningDialog(
                project,
                "OpenLinker could not find the selected browser: ${fallbackName(normalizedPreference)}.\n" +
                    "The link will open with the default browser.",
                OpenLinkerConstants.PLUGIN_NAME,
            )
        }

        BrowserLauncher.instance.browse(url, browser, project)
    }

    private fun fallbackName(preference: OpenLinkerBrowserPreference): String {
        return preference.browserName.ifBlank { preference.browserId }
    }

    private fun <T> withBrowserManager(block: (WebBrowserManager) -> T): T? {
        if (ApplicationManager.getApplication() == null) {
            return null
        }

        return runCatching { block(WebBrowserManager.getInstance()) }.getOrNull()
    }
}
