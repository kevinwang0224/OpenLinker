package com.openlinker.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import com.openlinker.OpenLinkerConstants
import com.openlinker.OpenLinkerEntryPopup
import com.openlinker.OpenLinkerIcons
import java.awt.event.MouseEvent
import javax.swing.Icon

class OpenLinkerStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = OpenLinkerConstants.PLUGIN_NAME

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun isEnabledByDefault(): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = OpenLinkerStatusBarWidget(project)

    private class OpenLinkerStatusBarWidget(private val project: Project) :
        StatusBarWidget,
        StatusBarWidget.IconPresentation {

        override fun ID(): String = WIDGET_ID

        override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

        override fun getTooltipText(): String = "OpenLinker"

        override fun getClickConsumer(): Consumer<MouseEvent> = Consumer { event ->
            OpenLinkerEntryPopup.showForMouseEvent(project, event)
        }

        override fun getIcon(): Icon = OpenLinkerIcons.BROWSER_OPEN
    }

    private companion object {
        const val WIDGET_ID = "OpenLinkerStatusBarWidget"
    }
}
