package com.light.import_image_tools.settings

import com.intellij.openapi.options.Configurable
import com.intellij.util.xmlb.XmlSerializerUtil
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
        return mySettingsComponent?.getRules() != settings.importRules ||
                mySettingsComponent?.scaleMappings != settings.scaleMappings
    }

    override fun apply() {
        val settings = AppSettingsState.instance
        settings.importRules = mySettingsComponent?.getRules()?.toMutableList() ?: mutableListOf()
        settings.scaleMappings = mySettingsComponent?.scaleMappings ?: ""
    }

    override fun reset() {
        val settings = AppSettingsState.instance
        mySettingsComponent?.setRules(settings.importRules)
        mySettingsComponent?.scaleMappings = settings.scaleMappings
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
