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

    fun groupImages(files: List<VirtualFile>, scaleMappings: String, rules: List<ImageImportRule>): List<GroupedImage> {
        val allFilesRaw = files.flatMap { if (it.isDirectory) it.children.toList() else listOf(it) }

        val allFiles = allFilesRaw.filter { file ->
            val extension = file.extension?.lowercase(Locale.getDefault())
            rules.any { rule ->
                rule.extensions.split(',').map { ext -> ext.trim().lowercase(Locale.getDefault()) }.contains(extension)
            }
        }

        val scaleMap = parseScaleMappings(scaleMappings)
        val sortedSuffixes = scaleMap.keys.sortedByDescending { it.length }

        val filesWithBaseName = allFiles.map { file ->
            var baseName = file.nameWithoutExtension
            for (suffix in sortedSuffixes) {
                if (file.nameWithoutExtension.endsWith(suffix)) {
                    baseName = file.nameWithoutExtension.removeSuffix(suffix)
                    break
                }
            }
            Triple(baseName, file.extension ?: "", file)
        }

        return filesWithBaseName.groupBy { it.first + "." + it.second }
            .map { (key, triples) ->
                val first = triples.first()
                val baseName = first.first
                val extension = first.second
                val fileList = triples.map { it.third }
                val rule = rules.find { rule ->
                    rule.extensions.split(',').map { ext -> ext.trim().lowercase(Locale.getDefault()) }.contains(extension)
                }!!
                GroupedImage(
                    files = fileList,
                    newName = "$baseName.${extension}",
                    baseName = baseName,
                    extension = extension,
                    rule = rule
                )
            }
    }

    private fun parseScaleMappings(mappings: String): Map<String, String> {
        return mappings.lines()
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0].trim() to parts[1].trim()
            }
    }

    fun importGroupedImages(project: Project, groupedImages: List<GroupedImage>) {
        val settings = AppSettingsState.instance
        val projectBasePath = project.basePath ?: return
        val scaleMap = parseScaleMappings(settings.scaleMappings)

        val groupedByRule = groupedImages.mapNotNull { groupedImage ->
            val extension = groupedImage.extension.lowercase(Locale.getDefault())
            val rule = settings.importRules.find {
                it.extensions.split(',').map { ext -> ext.trim().lowercase(Locale.getDefault()) }.contains(extension)
            }
            if (rule != null) Pair(rule, groupedImage) else null
        }.groupBy({ it.first }, { it.second })

        for ((rule, ruleGroups) in groupedByRule) {
            val unpastedCode = mutableListOf<String>()
            for (groupedImage in ruleGroups) {
                val targetBaseDir = File("$projectBasePath/${rule.targetDirectory}")
                if (!targetBaseDir.exists()) {
                    targetBaseDir.mkdirs()
                }

                val newBaseName = groupedImage.newName.substringBeforeLast('.')
                val newExtension = groupedImage.newName.substringAfterLast('.')

                for (file in groupedImage.files) {
                    var targetDir = targetBaseDir
                    val newFileName = "$newBaseName.$newExtension"

                    if (rule.applyScaling) {
                        val sortedSuffixes = scaleMap.keys.sortedByDescending { it.length }
                        for (suffix in sortedSuffixes) {
                            if (file.nameWithoutExtension.endsWith(suffix)) {
                                val dirName = scaleMap[suffix]!!
                                val scaleDir = File(targetBaseDir, dirName)
                                if (!scaleDir.exists()) scaleDir.mkdirs()
                                targetDir = scaleDir
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
                }
                val variableName = newBaseName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                val finalFileName = "$newBaseName.$newExtension"
                val baseTargetFile = File(targetBaseDir, finalFileName)
                val relativePath = baseTargetFile.path.removePrefix("$projectBasePath/").removePrefix("/")
                val codeLine = rule.codeTemplate
                    .replace("\${VARIABLE_NAME}", variableName)
                    .replace("\${FILE_NAME}", finalFileName)
                    .replace("\${RELATIVE_PATH}", relativePath)

                if (rule.pasteTarget.isNotBlank()) {
                    if (!upsertCodeDeclaration(project, rule, variableName, codeLine)) {
                        unpastedCode.add(codeLine)
                    }
                } else {
                    unpastedCode.add(codeLine)
                }
            }

            if (unpastedCode.isNotEmpty()) {
                GeneratedCodeDialog(project, unpastedCode.joinToString("\n")).show()
            }
        }
    }

    fun importImages(project: Project, files: List<VirtualFile>) {
        val settings = AppSettingsState.instance
        val projectBasePath = project.basePath ?: return
        val scaleMap = parseScaleMappings(settings.scaleMappings)
        
        val allFiles = files.flatMap { if (it.isDirectory) it.children.toList() else listOf(it) }

        // Group files by the rule they match
        val groupedByRule = allFiles.mapNotNull { file ->
            val extension = file.extension?.lowercase(Locale.getDefault())
            val rule = settings.importRules.find {
                it.extensions.split(',').map { ext -> ext.trim().lowercase(Locale.getDefault()) }.contains(extension)
            }
            if (rule != null) Pair(rule, file) else null
        }.groupBy({ it.first }, { it.second })


        for ((rule, ruleFiles) in groupedByRule) {
            val unpastedCode = mutableListOf<String>()
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

                    if (rule.pasteTarget.isNotBlank()) {
                        if (!upsertCodeDeclaration(project, rule, variableName, codeLine)) {
                            unpastedCode.add(codeLine)
                        }
                    } else {
                        unpastedCode.add(codeLine)
                    }
                }
            }

            if (unpastedCode.isNotEmpty()) {
                // Fallback to showing the dialog
                GeneratedCodeDialog(project, unpastedCode.joinToString("\n")).show()
            }
        }
    }

    private fun upsertCodeDeclaration(project: Project, rule: ImageImportRule, variableName: String, codeLine: String): Boolean {
        if (rule.pasteTarget.isBlank()) return false

        val parts = rule.pasteTarget.split("::")
        if (parts.size < 2) return false

        val filePath = parts[0].trim()
        val anchor = parts[1].trim()
        val position = if (parts.size > 2) parts[2].trim().lowercase(Locale.getDefault()) else "after"

        val fileToModify = LocalFileSystem.getInstance().findFileByPath("${project.basePath}/$filePath") ?: return false
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(fileToModify) } ?: return false

        val text = document.text
        val regex = Regex("""\b${Regex.escape(variableName)}\b""")
        val match = regex.find(text)

        if (match != null) {
            // Found: replace the entire declaration (handle multi-line declarations)
            WriteCommandAction.runWriteCommandAction(project) {
                val startLineNumber = document.getLineNumber(match.range.first)
                val lineStartOffset = document.getLineStartOffset(startLineNumber)
                
                // Get the indentation of the declaration line
                val declarationLine = text.substring(lineStartOffset, document.getLineEndOffset(startLineNumber))
                val baseIndent = declarationLine.takeWhile { it.isWhitespace() }.length
                
                // Find the end of the declaration
                var endOffset = match.range.first
                var currentLineNum = startLineNumber
                
                // First, try to find a semicolon on the same or following lines
                var foundSemicolon = false
                while (endOffset < text.length) {
                    val char = text[endOffset]
                    
                    if (char == ';') {
                        foundSemicolon = true
                        endOffset++
                        // Skip trailing whitespace on the same line
                        while (endOffset < text.length && text[endOffset] != '\n' && text[endOffset] != '\r' && text[endOffset].isWhitespace()) {
                            endOffset++
                        }
                        break
                    }
                    
                    if (char == '\n' || char == '\r') {
                        // Move to next line
                        if (char == '\r' && endOffset + 1 < text.length && text[endOffset + 1] == '\n') {
                            endOffset++ // Skip \r\n
                        }
                        endOffset++
                        currentLineNum++
                        
                        if (currentLineNum >= document.lineCount) break
                        
                        val nextLineStart = document.getLineStartOffset(currentLineNum)
                        val nextLineEnd = document.getLineEndOffset(currentLineNum)
                        val nextLine = text.substring(nextLineStart, nextLineEnd)
                        
                        // Check if next line is empty or has less/equal indentation (and is not just whitespace)
                        val nextLineIndent = nextLine.takeWhile { it.isWhitespace() }.length
                        val nextLineContent = nextLine.trim()
                        
                        if (nextLineContent.isEmpty()) {
                            // Empty line, continue to check next line
                            continue
                        } else if (nextLineIndent <= baseIndent) {
                            // Next line has same or less indentation and has content - declaration ended
                            break
                        }
                        // Continue if next line has more indentation (continuation of declaration)
                    } else {
                        endOffset++
                    }
                }
                
                // If no semicolon found and we're still on the first line, just use end of line
                if (!foundSemicolon && currentLineNum == startLineNumber) {
                    endOffset = document.getLineEndOffset(startLineNumber)
                }
                
                // Skip trailing newlines
                while (endOffset < text.length && (text[endOffset] == '\n' || text[endOffset] == '\r')) {
                    endOffset++
                }
                
                document.replaceString(lineStartOffset, endOffset, "$codeLine\n")
            }
            return true
        }

        // Not found, so we try to insert.
        val anchorIndex = text.indexOf(anchor)
        if (anchorIndex == -1) return false // Anchor not found, can't insert.

        WriteCommandAction.runWriteCommandAction(project) {
            val anchorLineNumber = document.getLineNumber(anchorIndex)
            val targetLineNumber = if (position == "before") anchorLineNumber else anchorLineNumber + 1
            val offset = document.getLineStartOffset(targetLineNumber)
            document.insertString(offset, "$codeLine\n")
        }
        return true
    }
}
