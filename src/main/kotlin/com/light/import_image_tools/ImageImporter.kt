package com.light.import_image_tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.light.import_image_tools.settings.AppSettingsState
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

    fun importImages(project: Project, files: List<VirtualFile>): List<String> {
        val settings = AppSettingsState.instance
        val projectBasePath = project.basePath ?: return emptyList()

        val rasterExts = settings.rasterImageExtensions.split(',').map { it.trim().toLowerCase() }.toSet()
        val vectorExts = settings.vectorImageExtensions.split(',').map { it.trim().toLowerCase() }.toSet()
        
        val scaleMap = parseScaleMappings(settings.scaleMappings)

        val allFiles = files.flatMap { if (it.isDirectory) it.children.toList() else listOf(it) }

        val generatedCode = mutableListOf<String>()
        val processedBaseNames = mutableSetOf<String>()

        for (file in allFiles) {
            val extension = file.extension?.toLowerCase() ?: continue

            val isRaster = extension in rasterExts
            val isVector = extension in vectorExts

            if (!isRaster && !isVector) {
                continue // Skip unsupported files
            }

            val targetPath = if (isRaster) settings.rasterPath else settings.vectorPath
            val targetBaseDir = File("$projectBasePath/$targetPath")
            if (!targetBaseDir.exists()) {
                targetBaseDir.mkdirs()
            }
            
            var targetDir = targetBaseDir
            var newFileName = file.name
            var baseName = file.nameWithoutExtension

            // Apply scaling logic only for raster images
            if (isRaster) {
                val sortedSuffixes = scaleMap.keys.sortedByDescending { it.length }
                for (suffix in sortedSuffixes) {
                    if (file.nameWithoutExtension.endsWith(suffix)) {
                        val dirName = scaleMap[suffix]!!
                        val scaleDir = File(targetBaseDir, dirName)
                        if (!scaleDir.exists()) {
                            scaleDir.mkdirs()
                        }
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
                val variableName = baseName.toUpperCase(Locale.ROOT)
                val relativePath = targetFile.path.removePrefix("$projectBasePath/").removePrefix("/")
                val codeLine = settings.codeTemplate
                    .replace("\${VARIABLE_NAME}", variableName)
                    .replace("\${RELATIVE_PATH}", relativePath)
                generatedCode.add(codeLine)
            }
        }
        return generatedCode
    }
}
