package com.light.import_image_tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.SVGLoader
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.table.AbstractTableModel

data class GroupedImage(
    val files: List<VirtualFile>,
    var newName: String,
    val baseName: String,
    val extension: String
)

class RenameImagesDialog(
    project: Project?,
    private val groupedImages: List<GroupedImage>
) : DialogWrapper(project) {

    private val tableModel = RenameTableModel(groupedImages)
    private val table = JBTable(tableModel)
    private val previewPanel = JPanel(BorderLayout())
    private val previewLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.CENTER
    }

    init {
        title = "重命名&预览"
        previewPanel.add(previewLabel, BorderLayout.CENTER)
        previewPanel.preferredSize = Dimension(300, 200)

        table.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val selectedRow = table.selectedRow
                if (selectedRow != -1) {
                    val modelRow = table.convertRowIndexToModel(selectedRow)
                    updatePreview(groupedImages[modelRow])
                } else {
                    updatePreview(null)
                }
            }
        }

        init()

        if (table.rowCount > 0) {
            SwingUtilities.invokeLater {
                table.setRowSelectionInterval(0, 0)
            }
        }
    }

    override fun getInitialSize(): Dimension {
        return Dimension(800, 500)
    }

    override fun createCenterPanel(): JComponent {
        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(400, 300)
        previewPanel.preferredSize = Dimension(300, 300)

        val splitPane = OnePixelSplitter(false) // false for horizontal split
        splitPane.firstComponent = scrollPane
        splitPane.secondComponent = previewPanel
        splitPane.proportion = 0.5f
        return splitPane
    }

    private fun updatePreview(groupedImage: GroupedImage?) {
        previewLabel.icon = null
        previewLabel.text = ""

        if (groupedImage == null) {
            return
        }

        val file = groupedImage.files.firstOrNull() ?: return
        val extension = file.extension?.lowercase()

        try {
            val image: Image? = when (extension) {
                "svg" -> SVGLoader.load(null, file.inputStream, 2.0f)
                "png", "jpg", "jpeg", "gif", "bmp" -> ImageIO.read(file.inputStream)
                else -> null
            }

            if (image != null) {
                // Scale image to fit the label, maintaining aspect ratio
                val panelWidth = previewPanel.width.takeIf { it > 0 } ?: 200
                val panelHeight = previewPanel.height.takeIf { it > 0 } ?: 200
                val scaledImage = scaleImage(image, panelWidth, panelHeight)
                previewLabel.icon = ImageIcon(scaledImage)
            } else {
                previewLabel.text = "<html><center>不支持查看该格式</center></html>"
            }
        } catch (e: Exception) {
            previewLabel.text = "<html><center>无法加载预览</center></html>"
            e.printStackTrace()
        }
    }

    private fun scaleImage(image: Image, maxWidth: Int, maxHeight: Int): Image {
        val imageWidth = image.getWidth(null)
        val imageHeight = image.getHeight(null)
        if (imageWidth <= 0 || imageHeight <= 0) return image

        val ratio = imageWidth.toDouble() / imageHeight.toDouble()

        var newWidth = imageWidth
        var newHeight = imageHeight

        if (imageWidth > maxWidth) {
            newWidth = maxWidth
            newHeight = (newWidth / ratio).toInt()
        }

        if (newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (newHeight * ratio).toInt()
        }

        return image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
    }

    fun getRenamedImages(): List<GroupedImage> {
        return groupedImages
    }
}

class RenameTableModel(private val groupedImages: List<GroupedImage>) : AbstractTableModel() {
    private val columnNames = arrayOf("文件名")

    override fun getRowCount(): Int = groupedImages.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 0

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return groupedImages[rowIndex].newName
    }

    override fun setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int) {
        if (aValue is String) {
            groupedImages[rowIndex].newName = aValue
        }
    }
}
