package com.openlinker

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object OpenLinkerIcons {
    @JvmField
    val BROWSER_OPEN: Icon = IconLoader.getIcon("/icons/openlinkerBrowserOpen.svg", OpenLinkerIcons::class.java)

    @JvmField
    val RULE_IMPORT: Icon = IconLoader.getIcon("/icons/openlinkerRuleImport.svg", OpenLinkerIcons::class.java)

    @JvmField
    val RULE_EXPORT_SELECTED: Icon =
        IconLoader.getIcon("/icons/openlinkerRuleExportSelected.svg", OpenLinkerIcons::class.java)

    @JvmField
    val RULE_EXPORT_ALL: Icon = IconLoader.getIcon("/icons/openlinkerRuleExportAll.svg", OpenLinkerIcons::class.java)
}
