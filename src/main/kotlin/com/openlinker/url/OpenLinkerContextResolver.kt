package com.openlinker.url

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project

object OpenLinkerContextResolver {
    fun resolve(project: Project, event: AnActionEvent): OpenLinkerContext {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        val module = event.getData(LangDataKeys.MODULE)
            ?: file?.let { ModuleUtilCore.findModuleForFile(it, project) }
            ?: ModuleManager.getInstance(project).modules.singleOrNull()

        return buildContext(project, file?.name.orEmpty(), file?.path.orEmpty(), module?.name.orEmpty())
    }

    fun resolve(project: Project): OpenLinkerContext {
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        val module = file?.let { ModuleUtilCore.findModuleForFile(it, project) }
            ?: ModuleManager.getInstance(project).modules.singleOrNull()

        return buildContext(project, file?.name.orEmpty(), file?.path.orEmpty(), module?.name.orEmpty())
    }

    private fun buildContext(
        project: Project,
        fileName: String,
        filePath: String,
        moduleName: String,
    ): OpenLinkerContext {
        return OpenLinkerContext(
            projectName = project.name,
            moduleName = moduleName,
            fileName = fileName,
            filePath = filePath,
        )
    }
}
