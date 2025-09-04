package com.light.import_image_tools.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.light.import_image_tools.settings.AppSettingsState",
    storages = [Storage("ImportImageToolsPlugin.xml")]
)
data class AppSettingsState(
    var rasterPath: String = "lib/resources/images",
    var rasterImageExtensions: String = "png, jpg, jpeg",
    var vectorPath: String = "lib/resources/svgs",
    var vectorImageExtensions: String = "svg",
    var scaleMappings: String = "@3x=3.0x\n@2x=2.0x",
    var codeTemplate: String = "val \${VARIABLE_NAME} = \"\${RELATIVE_PATH}\""
) : PersistentStateComponent<AppSettingsState> {

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: AppSettingsState
            get() = ApplicationManager.getApplication().getService(AppSettingsState::class.java)
    }
}
