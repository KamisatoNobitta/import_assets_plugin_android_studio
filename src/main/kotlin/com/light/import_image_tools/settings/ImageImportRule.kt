package com.light.import_image_tools.settings

import com.intellij.util.xmlb.annotations.Attribute
import java.util.*

data class ImageImportRule(
    @Attribute var id: String = UUID.randomUUID().toString(),
    @Attribute var name: String = "New Rule",
    @Attribute var extensions: String = "png, jpg",
    @Attribute var targetDirectory: String = "lib/resources/images",
    @Attribute var codeTemplate: String = "val \${VARIABLE_NAME} = \"\${RELATIVE_PATH}\"",
    @Attribute var applyScaling: Boolean = true,
    @Attribute var pasteTarget: String = ""
)
