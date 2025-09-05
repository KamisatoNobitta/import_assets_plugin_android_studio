package com.light.import_image_tools.settings

import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel

class AppSettingsComponent {

    val panel: JComponent
    private val rulesTableModel = RulesTableModel(mutableListOf())
    private val rulesTable = JBTable(rulesTableModel)
    private val scaleMappingsComponent = JBTextArea(5, 40)
    private val showRenameDialogCheckBox = JCheckBox("导入前弹出重命名弹窗")

    init {
        // Table setup
        rulesTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        // Use IntelliJ's specific renderer and editor for boolean columns to ensure
        // proper event handling and UI consistency within the settings panel.
        val booleanColumnIndex = 4
        val booleanColumn = rulesTable.getColumnModel().getColumn(booleanColumnIndex)
        booleanColumn.setCellRenderer(BooleanTableCellRenderer())
        booleanColumn.setCellEditor(BooleanTableCellEditor())

        val decorator = ToolbarDecorator.createDecorator(rulesTable)
            .setAddAction { rulesTableModel.addRow() }
            .setRemoveAction { rulesTableModel.removeRow(rulesTable.selectedRow) }

        val decoratedTable = decorator.createPanel()
        // Allow the table to shrink and grow naturally with the available space.
        decoratedTable.preferredSize = JBDimension(600, 200)

        val tablePanel = JPanel(BorderLayout(0, 5))
        tablePanel.add(JBLabel("导入规则 (Import Rules):"), BorderLayout.NORTH)
        tablePanel.add(decoratedTable, BorderLayout.CENTER)

        // show rename dialog
        val showRenameDialogPanel = JPanel(BorderLayout(0, 5))
        showRenameDialogPanel.add(showRenameDialogCheckBox, BorderLayout.WEST)

        // Scale Mappings setup
        val scaleMappingsPanel = JPanel(BorderLayout(0, 5))
        scaleMappingsPanel.add(JBLabel("缩放规则映射 (Scale Mappings):"), BorderLayout.NORTH)
        scaleMappingsPanel.add(JScrollPane(scaleMappingsComponent), BorderLayout.CENTER)
        scaleMappingsPanel.border = EmptyBorder(0, 0, 10, 0) // Add some space below

        // Description Panel setup
        val descriptionText = """
            <html>
            <b>使用说明:</b>
            <ul>
                <li>插件将从上到下查找<b>第一个匹配文件后缀</b>的规则来处理导入的文件。</li>
                <li><b>目标文件夹:</b> 相对于项目根目录的路径, 例如: <code>lib/resources/images</code></li>
                <li><b>识别三倍图:</b> 若勾选, 将使用缩放规则映射(Scale Mappings)进行识别 (例如 @2x, @3x)。</li>
                <li><b>代码模板可用占位符:</b>
                    <ul>
                        <li><code>${"$"}{VARIABLE_NAME}</code>: 根据文件名生成的变量名 (首字母小写驼峰)。</li>
                        <li><code>${"$"}{RELATIVE_PATH}</code>: 文件导入后相对于项目根目录的完整路径。</li>
                        <li><code>${"$"}{FILE_NAME}</code>: 导入后包含后缀的完整文件名。</li>
                    </ul>
                </li>
                 <li><b>自动粘贴目标 (可选):</b>
                    <ul>
                        <li>格式: <code>文件路径::锚点文本::[before|after]</code></li>
                        <li><b>文件路径:</b> 必填, 相对于项目根目录, 如 <code>src/R.kt</code></li>
                        <li><b>锚点文本:</b> 必填, 文件中用于定位的唯一文本, 如 <code>// ANCHOR</code></li>
                        <li><b>粘贴位置:</b> 可选, <code>before</code> 或 <code>after</code>, 默认为 <code>after</code>。</li>
                        <li>如果留空, 插件将弹出代码复制窗口。</li>
                        <li><b>样例 </b>放在<code>//gif_end</code>的前面: <code>lib/common/medias.dart:://gif_end::before</code></li>
                        <li><b>样例 </b>放在<code>class ImageNames {</code>的后面: <code>lib/common/medias.dart::class ImageNames {</code></li>
                    </ul>
                </li>
            </ul>
            </html>
        """.trimIndent()
        val descriptionLabel = JBLabel(descriptionText)

        val southPanel = JPanel(BorderLayout(0, 10))
        southPanel.add(scaleMappingsPanel, BorderLayout.NORTH)
        southPanel.add(descriptionLabel, BorderLayout.SOUTH)

        // Main content panel that holds all components
        val contentPanel = JPanel(BorderLayout(0, 10))
        contentPanel.add(showRenameDialogPanel, BorderLayout.NORTH)
        contentPanel.add(tablePanel, BorderLayout.CENTER)
        contentPanel.add(southPanel, BorderLayout.SOUTH)
        contentPanel.border = JBUI.Borders.empty(10)

        // The main panel is now a scroll pane to allow for vertical scrolling.
        panel = JScrollPane(contentPanel)
        panel.border = JBUI.Borders.empty()
    }

    val preferredFocusedComponent: JComponent
        get() = rulesTable

    var showRenameDialog: Boolean
        get() = showRenameDialogCheckBox.isSelected
        set(value) {
            showRenameDialogCheckBox.isSelected = value
        }

    var scaleMappings: String
        get() = scaleMappingsComponent.text
        set(value) {
            scaleMappingsComponent.text = value
        }

    fun getRules(): List<ImageImportRule> = rulesTableModel.getData()

    fun setRules(rules: List<ImageImportRule>) {
        rulesTableModel.setData(rules)
    }
}

class RulesTableModel(private var rules: MutableList<ImageImportRule>) : AbstractTableModel() {

    private val columnNames = arrayOf("名称", "文件后缀", "目标文件夹", "代码模板", "识别三倍图", "自动粘贴目标")

    override fun getRowCount(): Int = rules.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]
    override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
            4 -> Boolean::class.java
            else -> String::class.java
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val rule = rules[rowIndex]
        return when (columnIndex) {
            0 -> rule.name
            1 -> rule.extensions
            2 -> rule.targetDirectory
            3 -> rule.codeTemplate
            4 -> rule.applyScaling
            5 -> rule.pasteTarget
            else -> ""
        }
    }

    override fun setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int) {
        val rule = rules[rowIndex]
        when (columnIndex) {
            0 -> rule.name = aValue as String
            1 -> rule.extensions = aValue as String
            2 -> rule.targetDirectory = aValue as String
            3 -> rule.codeTemplate = aValue as String
            4 -> rule.applyScaling = aValue as Boolean
            5 -> rule.pasteTarget = aValue as String
        }
        // With the correct cell editor, the more efficient cell-specific update event is sufficient.
        fireTableCellUpdated(rowIndex, columnIndex)
    }

    fun addRow() {
        rules.add(ImageImportRule())
        fireTableRowsInserted(rules.size - 1, rules.size - 1)
    }

    fun removeRow(rowIndex: Int) {
        if (rowIndex >= 0 && rowIndex < rules.size) {
            rules.removeAt(rowIndex)
            fireTableRowsDeleted(rowIndex, rowIndex)
        }
    }

    fun setData(data: List<ImageImportRule>) {
        // Create a deep copy of the rules to avoid modifying the original state directly.
        // This is crucial for the settings UI to correctly detect changes.
        rules = data.map { it.copy() }.toMutableList()
        fireTableDataChanged()
    }

    fun getData(): List<ImageImportRule> {
        return rules
    }
}
