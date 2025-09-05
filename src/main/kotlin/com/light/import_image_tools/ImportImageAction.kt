package com.light.import_image_tools

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VfsUtil
import com.light.import_image_tools.settings.AppSettingsState

class ImportImageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val descriptor = FileChooserDescriptor(
            true, // choose files
            true, // choose folders
            false, // choose jars
            false, // choose jar contents
            false, // choose archives
            true // choose multiple
        ).withTitle("Select Images or Folder to Import")
            .withDescription("Select PNG/SVG files or a folder containing them.")

        FileChooser.chooseFiles(descriptor, project, null) { selectedFiles ->
            if (selectedFiles.isNotEmpty()) {
                val settings = AppSettingsState.instance
                if (settings.showRenameDialog) {
                    val groupedImages = ImageImporter.groupImages(selectedFiles, settings.scaleMappings, settings.importRules)
                    if (groupedImages.isEmpty()) return@chooseFiles

                    val dialog = RenameImagesDialog(project, groupedImages)
                    if (dialog.showAndGet()) {
                        val renamedImages = dialog.getRenamedImages()
                        ImageImporter.importGroupedImages(project, renamedImages)
                    }
                } else {
                    ImageImporter.importImages(project, selectedFiles)
                }

                // Refresh the project view to show the new files, including the pasted code.
                VfsUtil.markDirtyAndRefresh(true, true, true, project.baseDir)
            }
        }
    }
}
