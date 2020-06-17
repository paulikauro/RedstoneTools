package redstonetools

import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.format.TextColor

fun TextComponent.getAllContent(): String =
    if (this.children().isEmpty()) {
        this.content()
    } else {
        this.children().filterIsInstance<TextComponent>().joinToString(separator = "") { textComponent ->
            textComponent.getAllContent()
        }
    }

fun String.getHighlightedReplacement(replacement: String): TextComponent =
    TextComponent.of(this.substringBefore(replacement))
        .color(TextColor.WHITE)
        .append(
            TextComponent.of(replacement)
                .color(TextColor.YELLOW)
        )
        .append(
            TextComponent.of(this.substringAfter(replacement))
                .color(TextColor.WHITE)
        )
