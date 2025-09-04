package com.light.import_image_tools

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.light.import_image_tools.settings.AppSettingsState
import com.light.import_image_tools.settings.ImageImportRule
import java.io.File
import java.util.*

object ImageImporter {

    private fun parseScaleMappings(mappings: String): Map<String, String> {
        return mappings.lines()
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0].trim() to parts[1].trim()
            }
    }

    fun importImages(project: Project, files: List<VirtualFile>) {
        val settings = AppSettingsState.instance
        val projectBasePath = project.basePath ?: return
        val scaleMap = parseScaleMappings(settings.scaleMappings)
        
        val allFiles = files.flatMap { if (it.isDirectory) it.children.toList() else listOf(it) }

        // Group files by the rule they match
        val groupedByRule = allFiles.mapNotNull { file ->
            val extension = file.extension?.toLowerCase()
            val rule = settings.importRules.find {
                it.extensions.split(',').map { ext -> ext.trim().toLowerCase() }.contains(extension)
            }
            if (rule != null) Pair(rule, file) else null
        }.groupBy({ it.first }, { it.second })


        for ((rule, ruleFiles) in groupedByRule) {
            val generatedCode = mutableListOf<String>()
            val processedBaseNames = mutableSetOf<String>()

            for (file in ruleFiles) {
                val targetBaseDir = File("$projectBasePath/${rule.targetDirectory}")
                if (!targetBaseDir.exists()) {
                    targetBaseDir.mkdirs()
                }

                var targetDir = targetBaseDir
                var newFileName = file.name
                var baseName = file.nameWithoutExtension

                if (rule.applyScaling) {
                    val sortedSuffixes = scaleMap.keys.sortedByDescending { it.length }
                    for (suffix in sortedSuffixes) {
                        if (file.nameWithoutExtension.endsWith(suffix)) {
                            val dirName = scaleMap[suffix]!!
                            val scaleDir = File(targetBaseDir, dirName)
                            if (!scaleDir.exists()) scaleDir.mkdirs()
                            targetDir = scaleDir
                            baseName = file.nameWithoutExtension.removeSuffix(suffix)
                            newFileName = "$baseName.${file.extension}"
                            break
                        }
                    }
                }

                val targetFile = File(targetDir, newFileName)
                file.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (processedBaseNames.add(baseName)) {
                    val variableName = baseName.replaceFirstChar { it.lowercase(Locale.ROOT) }
                    val relativePath = targetFile.path.removePrefix("$projectBasePath/").removePrefix("/")
                    val codeLine = rule.codeTemplate
                        .replace("\${VARIABLE_NAME}", variableName)
                        .replace("\${RELATIVE_PATH}", relativePath)
                        .replace("\${FILE_NAME}", newFileName)
                    generatedCode.add(codeLine)
                }
            }

            if (generatedCode.isNotEmpty()) {
                val success = tryAutoPaste(project, rule, generatedCode)
                if (!success) {
                    // Fallback to showing the dialog
                    GeneratedCodeDialog(project, generatedCode.joinToString("\n")).show()
                }
            }
        }
    }

    private fun tryAutoPaste(project: Project, rule: ImageImportRule, codeLines: List<String>): Boolean {
        if (rule.pasteTarget.isBlank()) return false
        
        val parts = rule.pasteTarget.split("::")
        if (parts.size < 2) return false

        val filePath = parts[0].trim()
        val anchor = parts[1].trim()
        val position = if (parts.size > 2) parts[2].trim().toLowerCase() else "after"

        val fileToModify = LocalFileSystem.getInstance().findFileByPath("${project.basePath}/$filePath") ?: return false
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(fileToModify) } ?: return false
        
        val text = document.text
        val anchorIndex = text.indexOf(anchor)
        if (anchorIndex == -1) return false
        
        val anchorLineNumber = document.getLineNumber(anchorIndex)
        val targetLineNumber = if (position == "before") anchorLineNumber else anchorLineNumber + 1
        
        val textToInsert = codeLines.joinToString("\n", postfix = "\n")
        val offset = document.getLineStartOffset(targetLineNumber)

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(offset, textToInsert)
        }
        return true
    }
}
