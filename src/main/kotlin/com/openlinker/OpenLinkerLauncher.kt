package com.openlinker

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.openlinker.browser.OpenLinkerBrowsers
import com.openlinker.model.CustomUrlRule
import com.openlinker.settings.OpenLinkerConfigurable
import com.openlinker.settings.OpenLinkerSettingsService
import com.openlinker.url.OpenLinkerContext
import com.openlinker.url.OpenLinkerContextResolver
import com.openlinker.url.OpenLinkerRuleDecider
import com.openlinker.url.OpenLinkerRuleDecision
import com.openlinker.url.OpenLinkerUrlTemplateResolver
import java.nio.file.Files
import javax.swing.JList

object OpenLinkerLauncher {
    fun openFromAction(event: AnActionEvent) {
        val project = event.project ?: return
        val context = OpenLinkerContextResolver.resolve(project, event)
        openWithContext(project, context) { rules ->
            createRuleChooser(project, rules, context)
                .createPopup()
                .showInBestPositionFor(event.dataContext)
        }
    }

    fun openFromProject(project: Project) {
        val context = OpenLinkerContextResolver.resolve(project)
        openWithContext(project, context) { rules ->
            createRuleChooser(project, rules, context)
                .createPopup()
                .showCenteredInCurrentWindow(project)
        }
    }

    fun openRule(project: Project, rule: CustomUrlRule, context: OpenLinkerContext) {
        openResolvedRule(project, rule, context)
    }

    private fun openWithContext(
        project: Project,
        context: OpenLinkerContext,
        showChooser: (List<CustomUrlRule>) -> Unit,
    ) {
        val settings = OpenLinkerSettingsService.getInstance()
        when (val decision = OpenLinkerRuleDecider.decide(settings.getRules())) {
            OpenLinkerRuleDecision.NoEnabledRules -> showMissingRulesPrompt(project)
            is OpenLinkerRuleDecision.SingleRule -> openResolvedRule(project, decision.rule, context)
            is OpenLinkerRuleDecision.MultipleRules -> showChooser(decision.rules)
        }
    }

    private fun showMissingRulesPrompt(project: Project) {
        val result = Messages.showYesNoDialog(
            project,
            "OpenLinker has no enabled rules.\nOpen ${OpenLinkerConstants.SETTINGS_LOCATION} to add one?",
            OpenLinkerConstants.PLUGIN_NAME,
            "Open Settings",
            Messages.getCancelButton(),
            Messages.getInformationIcon(),
        )

        if (result == Messages.YES) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, OpenLinkerConfigurable::class.java)
        }
    }

    private fun createRuleChooser(
        project: Project,
        rules: List<CustomUrlRule>,
        context: OpenLinkerContext,
    ) = JBPopupFactory.getInstance()
        .createPopupChooserBuilder(rules)
        .setTitle(OpenLinkerConstants.PLUGIN_NAME)
        .setNamerForFiltering { it.name.ifBlank { it.urlTemplate } }
        .setRenderer(object : ColoredListCellRenderer<CustomUrlRule>() {
            override fun customizeCellRenderer(
                list: JList<out CustomUrlRule>,
                value: CustomUrlRule?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
            ) {
                if (value == null) {
                    return
                }

                append(value.name.ifBlank { value.urlTemplate })
                if (value.urlTemplate.isNotBlank()) {
                    append("  ")
                    append(value.urlTemplate, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        })
        .setItemChosenCallback { rule -> openResolvedRule(project, rule, context) }

    private fun openResolvedRule(project: Project, rule: CustomUrlRule, context: OpenLinkerContext) {
        val resolvedUrl = OpenLinkerUrlTemplateResolver.resolve(rule.urlTemplate, context).trim()
        if (!OpenLinkerResolvedUrl.canOpen(resolvedUrl)) {
            Messages.showWarningDialog(
                project,
                "OpenLinker could not build a usable address. Check the rule or the current file context in ${OpenLinkerConstants.SETTINGS_LOCATION}.",
                OpenLinkerConstants.PLUGIN_NAME,
            )
            return
        }

        try {
            val localPath = OpenLinkerResolvedUrl.toLocalPath(resolvedUrl)
            if (localPath != null) {
                if (!Files.exists(localPath)) {
                    Messages.showWarningDialog(
                        project,
                        "OpenLinker could not find this file:\n$localPath",
                        OpenLinkerConstants.PLUGIN_NAME,
                    )
                    return
                }

                if (Files.isDirectory(localPath)) {
                    RevealFileAction.openDirectory(localPath)
                } else {
                    RevealFileAction.openFile(localPath)
                }
                return
            }

            val settings = OpenLinkerSettingsService.getInstance()
            OpenLinkerBrowsers.browse(
                project,
                resolvedUrl,
                OpenLinkerBrowsers.effectivePreference(rule.browserPreference, settings.getGlobalBrowserPreference()),
            )
        } catch (_: Exception) {
            Messages.showErrorDialog(
                project,
                "OpenLinker could not open this address:\n$resolvedUrl",
                OpenLinkerConstants.PLUGIN_NAME,
            )
        }
    }
}
