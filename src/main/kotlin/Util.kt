package redstonetools

import com.sk89q.worldedit.util.formatting.text.TextComponent

fun TextComponent.getAllContent(): String {
    return if (this.children().isEmpty()) {
        this.content()
    } else {
        this.children().filterIsInstance<TextComponent>().joinToString(separator = "") { textComponent ->
            textComponent.getAllContent()
        }
    }
}
