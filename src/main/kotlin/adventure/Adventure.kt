package io.github.paulikauro.redstonetools.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.StyleBuilderApplicable

/** Concatenate [components] */
fun c(vararg components: Component): Component = Component.textOfChildren(*components)

/** Turns this string into a text component with [styles] */
fun String.c(vararg styles: StyleBuilderApplicable) = Component.text(this, Style.style(*styles))

/** Create a new component from [this] with [styles] applied */
fun Component.c(vararg styles: StyleBuilderApplicable) =
    this.style { builder -> styles.forEach { it.styleApply(builder) } }

/** Combine styles */
operator fun StyleBuilderApplicable.plus(other: StyleBuilderApplicable): StyleBuilderApplicable =
    Style.style(this, other).builderApplicable()

private fun Style.builderApplicable() = StyleBuilderApplicable { it.merge(this) }

fun buildTextComponent(f: TextComponent.Builder.() -> Unit): Component = Component.text().apply(f).build()

fun Iterable<Component>.join(
    separator: Component? = null,
    prefix: Component? = null,
    suffix: Component? = null,
    lastSeparator: Component? = null,
): Component = Component.join(
    JoinConfiguration.builder().separator(separator).prefix(prefix).suffix(suffix).lastSeparator(lastSeparator),
    this,
)

object Buttons {
    fun suggest(hover: Component, command: String): StyleBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.suggestCommand(command)

    fun run(hover: Component, command: String): StyleBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.runCommand(command)

    fun url(hover: Component, url: String): StyleBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.openUrl(url)
}
