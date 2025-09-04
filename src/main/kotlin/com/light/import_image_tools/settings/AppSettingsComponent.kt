package com.light.import_image_tools.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AppSettingsComponent {

    val panel: JPanel
    private val rasterPathField = JBTextField()
    private val rasterExtensionsField = JBTextField()
    private val vectorPathField = JBTextField()
    private val vectorExtensionsField = JBTextField()
    private val scaleMappingsComponent = JBTextArea(5, 40)
    private val codeTemplateComponent = JBTextField()

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Raster image path (for png, jpg...):"), rasterPathField, 1, false)
            .addLabeledComponent(JBLabel("Raster image extensions (comma-separated):"), rasterExtensionsField, 1, false)
            .addLabeledComponent(JBLabel("Vector image path (for svg...):"), vectorPathField, 1, false)
            .addLabeledComponent(JBLabel("Vector image extensions (comma-separated):"), vectorExtensionsField, 1, false)
            .addLabeledComponent(JBLabel("Scale mappings (suffix=directory):"), scaleMappingsComponent, 1, true)
            .addLabeledComponent(JBLabel("Code generation template(args:\${VARIABLE_NAME} \${RELATIVE_PATH} \${FILE_NAME}):"), codeTemplateComponent, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    val preferredFocusedComponent: JComponent
        get() = rasterPathField

    var rasterPath: String
        get() = rasterPathField.text
        set(value) {
            rasterPathField.text = value
        }

    var rasterExtensions: String
        get() = rasterExtensionsField.text
        set(value) {
            rasterExtensionsField.text = value
        }
    
    var vectorPath: String
        get() = vectorPathField.text
        set(value) {
            vectorPathField.text = value
        }

    var vectorExtensions: String
        get() = vectorExtensionsField.text
        set(value) {
            vectorExtensionsField.text = value
        }

    var scaleMappings: String
        get() = scaleMappingsComponent.text
        set(value) {
            scaleMappingsComponent.text = value
        }

    var codeTemplate: String
        get() = codeTemplateComponent.text
        set(value) {
            codeTemplateComponent.text = value
        }
}
