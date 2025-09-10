package com.light.import_image_tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.SVGLoader
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Image
import java.awt.event.KeyEvent
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel
import com.light.import_image_tools.settings.ImageImportRule

data class GroupedImage(
    val files: List<VirtualFile>,
    var newName: String,
    val baseName: String,
    val extension: String,
    val rule: ImageImportRule
)

class RenameImagesDialog(
    private val project: Project?,
    private val groupedImages: List<GroupedImage>
) : DialogWrapper(project) {

    private val tableModel = RenameTableModel(groupedImages)
    private val table = JBTable(tableModel)
    private val previewPanel = JPanel(BorderLayout())
    private val previewContentPanel = JPanel(BorderLayout())
    private val previewTitle = JBLabel("", SwingConstants.CENTER)

    init {
        title = "重命名&预览"

        // Change Enter key behavior to start editing
        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        table.getInputMap(JComponent.WHEN_FOCUSED).put(enterKeyStroke, "startEditing")

        previewPanel.add(previewTitle, BorderLayout.NORTH)
        previewPanel.add(previewContentPanel, BorderLayout.CENTER)
        previewPanel.preferredSize = Dimension(300, 200)

        table.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                handleSelectionChange()
            }
        }

        tableModel.addTableModelListener { e ->
            if (e.type == TableModelEvent.UPDATE) {
                handleSelectionChange()
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

    private fun handleSelectionChange() {
        val selectedRow = table.selectedRow
        if (selectedRow != -1) {
            val modelRow = table.convertRowIndexToModel(selectedRow)
            updatePreview(groupedImages[modelRow])
        } else {
            updatePreview(null)
        }
    }

    private fun updatePreview(groupedImage: GroupedImage?) {
        previewContentPanel.removeAll()

        if (groupedImage == null) {
            previewTitle.text = ""
            previewContentPanel.revalidate()
            previewContentPanel.repaint()
            return
        }

        val projectPath = project?.basePath ?: ""
        val targetFile = File(projectPath, groupedImage.rule.targetDirectory + "/" + groupedImage.newName)
        val existingVFile = LocalFileSystem.getInstance().findFileByIoFile(targetFile)
        val isConflict = existingVFile != null && !groupedImage.files.contains(existingVFile)

        if (isConflict) {
            previewTitle.text = "<html><center><b>文件已存在</b><br>左: 已有文件 -> 右: 新文件</center></html>"
            previewContentPanel.layout = GridLayout(1, 2, 5, 0)
            val existingImagePanel = createImagePreviewPanel(existingVFile!!)
            val newImagePanel = createImagePreviewPanel(groupedImage.files.first())
            previewContentPanel.add(existingImagePanel)
            previewContentPanel.add(newImagePanel)
        } else {
            previewTitle.text = "预览"
            previewContentPanel.layout = BorderLayout()
            val newImagePanel = createImagePreviewPanel(groupedImage.files.first())
            previewContentPanel.add(newImagePanel, BorderLayout.CENTER)
        }

        previewContentPanel.revalidate()
        previewContentPanel.repaint()
    }

    private fun createImagePreviewPanel(file: VirtualFile): JComponent {
        val panel = JPanel(BorderLayout())
        val label = JBLabel()
        label.horizontalAlignment = SwingConstants.CENTER
        label.verticalAlignment = SwingConstants.CENTER

        try {
            val image = loadImage(file)
            if (image != null) {
                // Defer scaling until the component is sized
                SwingUtilities.invokeLater {
                    val panelWidth = panel.width.takeIf { it > 0 } ?: 200
                    val panelHeight = panel.height.takeIf { it > 0 } ?: 200
                    label.icon = ImageIcon(scaleImage(image, panelWidth, panelHeight))
                }
            } else {
                label.text = "<html><center>不支持查看该格式</center></html>"
            }
        } catch (e: Exception) {
            label.text = "<html><center>无法加载预览</center></html>"
            e.printStackTrace()
        }

        panel.add(label, BorderLayout.CENTER)
        return panel
    }

    private fun loadImage(file: VirtualFile): Image? {
        return try {
            val extension = file.extension?.lowercase()
            when (extension) {
                "svg" -> SVGLoader.load(null, file.inputStream, 2.0f)
                "png", "jpg", "jpeg", "gif", "bmp" -> ImageIO.read(file.inputStream)
                else -> null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
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

    override fun doOKAction() {
        if (table.isEditing) {
            table.cellEditor.stopCellEditing()
        }
        super.doOKAction()
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
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }
}
