package com.light.import_image_tools.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class AppSettingsConfigurable : Configurable {

    private var mySettingsComponent: AppSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "Import Image Tools"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.instance
        return mySettingsComponent?.rasterPath != settings.rasterPath ||
                mySettingsComponent?.rasterExtensions != settings.rasterImageExtensions ||
                mySettingsComponent?.vectorPath != settings.vectorPath ||
                mySettingsComponent?.vectorExtensions != settings.vectorImageExtensions ||
                mySettingsComponent?.scaleMappings != settings.scaleMappings ||
                mySettingsComponent?.codeTemplate != settings.codeTemplate
    }

    override fun apply() {
        val settings = AppSettingsState.instance
        settings.rasterPath = mySettingsComponent?.rasterPath ?: ""
        settings.rasterImageExtensions = mySettingsComponent?.rasterExtensions ?: ""
        settings.vectorPath = mySettingsComponent?.vectorPath ?: ""
        settings.vectorImageExtensions = mySettingsComponent?.vectorExtensions ?: ""
        settings.scaleMappings = mySettingsComponent?.scaleMappings ?: ""
        settings.codeTemplate = mySettingsComponent?.codeTemplate ?: ""
    }

    override fun reset() {
        val settings = AppSettingsState.instance
        mySettingsComponent?.rasterPath = settings.rasterPath
        mySettingsComponent?.rasterExtensions = settings.rasterImageExtensions
        mySettingsComponent?.vectorPath = settings.vectorPath
        mySettingsComponent?.vectorExtensions = settings.vectorImageExtensions
        mySettingsComponent?.scaleMappings = settings.scaleMappings
        mySettingsComponent?.codeTemplate = settings.codeTemplate
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
