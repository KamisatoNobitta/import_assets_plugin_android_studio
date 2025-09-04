package com.light.import_image_tools

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class GeneratedCodeDialog(project: Project?, private val code: String) : DialogWrapper(project) {

    init {
        title = "Generated Code"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val textArea = JTextArea(code)
        textArea.isEditable = false
        textArea.caretPosition = 0 // Scroll to top

        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(600, 400)

        val panel = JPanel(BorderLayout())
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    override fun createActions(): Array<Action> {
        val copyAction = createAction("Copy and Close", KeyEvent.VK_C) {
            CopyPasteManager.getInstance().setContents(StringSelection(code))
            close(OK_EXIT_CODE)
        }
        return arrayOf(copyAction, cancelAction)
    }

    private fun createAction(name: String, mnemonic: Int, action: () -> Unit): Action {
        return object : DialogWrapperAction(name) {
            init {
                putValue(MNEMONIC_KEY, mnemonic)
            }
            override fun doAction(e: java.awt.event.ActionEvent) {
                action()
            }
        }
    }
}
