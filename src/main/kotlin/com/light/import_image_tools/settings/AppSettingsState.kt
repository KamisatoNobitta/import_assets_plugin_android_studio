package com.light.import_image_tools.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.XCollection

@State(
    name = "com.light.import_image_tools.settings.AppSettingsState",
    storages = [Storage("ImportImageToolsPlugin.xml")]
)
data class AppSettingsState(
    var scaleMappings: String = "@3x=3.0x\n@2x=2.0x",
    @XCollection
    var importRules: MutableList<ImageImportRule> = mutableListOf()
) : PersistentStateComponent<AppSettingsState> {

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    fun ensureDefaultRules() {
        if (importRules.isEmpty()) {
            importRules.add(
                ImageImportRule(
                    name = "Raster Images",
                    extensions = "png, jpg, jpeg",
                    targetDirectory = "lib/resources/images",
                    codeTemplate = "  static const String \${VARIABLE_NAME} = '\$imageBasePath/\${FILE_NAME}';",
                    applyScaling = true,
                    pasteTarget = "lib/common/images.dart::}::before"
                )
            )
            importRules.add(
                ImageImportRule(
                    name = "Vector Images",
                    extensions = "svg",
                    targetDirectory = "lib/resources/svgs",
                    codeTemplate = "  static const String \${VARIABLE_NAME} = '\$svgBasePath/\${FILE_NAME}';",
                    applyScaling = false,
                    pasteTarget = "lib/common/svgs.dart::}::before"
                )
            )
            importRules.add(
                ImageImportRule(
                    name = "GIF",
                    extensions = "gif",
                    targetDirectory = "lib/resources/gifs",
                    codeTemplate = "String \${VARIABLE_NAME} = _gifPath('\${FILE_NAME}');",
                    applyScaling = false,
                    pasteTarget = "lib/common/medias.dart:://gif_end::before"
                )
            )
        }
    }

    companion object {
        val instance: AppSettingsState
            get() = ApplicationManager.getApplication().getService(AppSettingsState::class.java).apply {
                ensureDefaultRules()
            }
    }
}
