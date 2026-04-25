package com.openlinker

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenLinkerToolsMenuAction : AnAction(
    OpenLinkerConstants.PLUGIN_NAME,
    "Open OpenLinker using configured rules",
    OpenLinkerIcons.BROWSER_OPEN,
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        OpenLinkerEntryPopup.showCenteredForAction(event)
    }
}
