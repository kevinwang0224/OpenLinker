package com.openlinker

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.ui.awt.RelativePoint
import com.openlinker.model.CustomUrlRule
import com.openlinker.settings.OpenLinkerConfigurable
import com.openlinker.settings.OpenLinkerSettingsService
import com.openlinker.url.OpenLinkerContext
import com.openlinker.url.OpenLinkerContextResolver
import java.awt.event.MouseEvent

object OpenLinkerEntryPopup {
    fun showForAction(event: AnActionEvent) {
        val project = event.project ?: return
        val context = OpenLinkerContextResolver.resolve(project, event)
        val popup = createPopup(project, context, event.dataContext)
        val inputEvent = event.inputEvent

        if (inputEvent is MouseEvent) {
            popup.show(RelativePoint(inputEvent))
        } else {
            popup.showInBestPositionFor(event.dataContext)
        }
    }

    fun showCenteredForAction(event: AnActionEvent) {
        val project = event.project ?: return
        val context = OpenLinkerContextResolver.resolve(project, event)
        createPopup(project, context, event.dataContext).showCenteredInCurrentWindow(project)
    }

    fun showForMouseEvent(project: Project, event: MouseEvent) {
        val dataContext = DataManager.getInstance().getDataContext(event.component, event.x, event.y)
        val context = OpenLinkerContextResolver.resolve(project)
        createPopup(project, context, dataContext).show(RelativePoint(event))
    }

    private fun createPopup(project: Project, context: OpenLinkerContext, dataContext: DataContext): ListPopup {
        val enabledRules = OpenLinkerSettingsService.getInstance().getEnabledRules()
        val group = DefaultActionGroup().apply {
            if (enabledRules.isEmpty()) {
                add(NoRulesAction())
            } else {
                enabledRules.forEach { rule ->
                    add(OpenRuleAction(project, context, rule))
                }
            }
            addSeparator()
            add(object : DumbAwareAction("Open Settings", "Edit OpenLinker rules", null) {
                override fun actionPerformed(event: AnActionEvent) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, OpenLinkerConfigurable::class.java)
                }
            })
        }

        return JBPopupFactory.getInstance().createActionGroupPopup(
            OpenLinkerConstants.PLUGIN_NAME,
            group,
            dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
        )
    }

    private class OpenRuleAction(
        private val project: Project,
        private val context: OpenLinkerContext,
        private val rule: CustomUrlRule,
    ) : DumbAwareAction(
        rule.name.ifBlank { rule.urlTemplateForProject(context.projectName) },
        rule.urlTemplateForProject(context.projectName),
        OpenLinkerIcons.BROWSER_OPEN,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            OpenLinkerLauncher.openRule(project, rule, context)
        }
    }

    private class NoRulesAction : DumbAwareAction("No enabled rules configured", null, null) {
        override fun actionPerformed(event: AnActionEvent) = Unit

        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.isEnabled = false
        }
    }
}
