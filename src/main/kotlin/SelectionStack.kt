package io.github.paulikauro.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.RegionSelector
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.world.World
import io.github.paulikauro.redstonetools.adventure.Buttons
import io.github.paulikauro.redstonetools.adventure.c
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor.BLUE
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.TextDecoration.BOLD
import org.bukkit.Bukkit
import java.util.*

fun PluginScope.createSelectionStack() {
    commandManager.registerCommand(SelectionStack())
}

private typealias Stack = MutableList<RegionSelector>
// indexed by world id
private typealias Stacks = MutableMap<Key, Stack>

private val World.key: Key get() = BukkitAdapter.adapt(this).key

@CommandAlias("/selstack")
@Description("Temporarily save your selection onto a stack")
@CommandPermission("redstonetools.selstack")
private class SelectionStack : BaseCommand() {
    private val stacks = mutableMapOf<UUID, Stacks>()

    private fun stackOf(player: UUID, worldId: Key): Stack =
        stacksOf(player).computeIfAbsent(worldId) { mutableListOf() }

    private fun stacksOf(player: UUID): Stacks = stacks.computeIfAbsent(player) { mutableMapOf() }

    @HelpCommand
    fun help(help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("push")
    @Description("Push selection")
    fun push(player: WEPlayer, session: LocalSession) {
        val selWorld = session.selectionWorld ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        val selector = session.getRegionSelector(selWorld)
        if (!selector.isDefined) throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        if (selWorld != player.world) {
            player.warn("Your selection is in ${selWorld.name}, which is not your current world.")
        }

        stackOf(player.uniqueId, selWorld.key).add(selector.clone())
        selector.clear()
        session.dispatchCUISelection(player)
        player.info("Selection pushed. Selection cleared.")
    }

    @Subcommand("pop")
    @Description("Pop selection")
    fun pop(player: WEPlayer, session: LocalSession) {
        val selector = stackOf(player.uniqueId, player.world.key).removeLastOrNull()
            ?: throw RedstoneToolsException("Your selection stack in world ${player.world.name} is empty")
        // it could be useful to warn if your selection in another world is overwritten, but idk
        session.setSelectorAndUpdateCUI(player, selector)
        player.info("Selection popped.")
    }

    fun parseWorld(idOrName: String): World = BukkitAdapter.adapt(
        Bukkit.getWorld(idOrName)
            ?: Bukkit.getWorld(Key.key(idOrName))
            ?: throw RedstoneToolsException("Cannot find world with name or id $idOrName")
    )

    @Subcommand("clear")
    @Description("Clear your selection stack")
    fun clear(player: WEPlayer, @Optional worldIdOrName: String?) {
        val world = if (worldIdOrName != null) {
            parseWorld(worldIdOrName)
        } else {
            player.world
        }
        // TODO: maybe can hover the world key? or not
        stackOf(player.uniqueId, world.key).clear()
        player.info("Your selection stack in world ${world.name} has been cleared")
    }

    @Subcommand("clearall")
    @Description("Clear your selection stacks in all worlds")
    fun clearAll(player: WEPlayer) {
        stacksOf(player.uniqueId).clear()
        player.info("Your selection stacks in all worlds have been cleared.")
    }

    @Default
    @Subcommand("show")
    @Description("Show your selection stack")
    fun show(player: WEPlayer) {
        val stacks = stacksOf(player.uniqueId)
        stacks.values.removeAll { it.isEmpty() }
        if (stacks.isEmpty()) {
            player.info("No stacks")
            return
        }
        for ((key, stack) in stacks.entries) {
            val world = Bukkit.getWorld(key) ?: run {
                player.err("Cannot access world $key")
                continue
            }
            if (stack.isEmpty()) continue
            player.info(c("In world ".c(), world.name.c(BOLD)))
            for ((i, selector) in stack.withIndex()) {
                val btnDelete = Buttons.run("Delete".c(), "//selstack delete ${world.key} $i")
                val btnSelect = Buttons.run("Select".c(), "//selstack select ${world.key} $i")
                val buttons = c("❌".c(btnDelete), " ".c(), "\uD83D\uDCCB".c(btnSelect))
                val stackPosInfo = when (i) {
                    0 if stack.size > 1 -> " (bottom of stack)"
                    stack.lastIndex -> " (top of stack)"
                    else -> ""
                }.c(GRAY)
                player.sendMessage(c(buttons, " ".c(), selector.typeName.c(BLUE), stackPosInfo))
                selector.selectionInfoLines.forEach { line ->
                    // adventure wen
                    player.print(TextComponent.of("  ").append(line))
                }
            }
        }
    }

    @Private
    @Subcommand("delete")
    fun delete(player: WEPlayer, worldId: String, index: Int) {
        val world = parseWorld(worldId)
        val stack = stackOf(player.uniqueId, world.key)
        if (index in stack.indices) {
            stack.removeAt(index)
        }
        // refresh listing since indices moved
        show(player)
    }

    @Private
    @Subcommand("select")
    fun select(player: WEPlayer, session: LocalSession, worldId: String, index: Int) {
        val world = parseWorld(worldId)
        if (world != player.world)
            throw RedstoneToolsException("You're in the wrong world, what you're trying to select is in ${world.name}")
        val stack = stackOf(player.uniqueId, world.key)
        val selector =
            stack.getOrNull(index) ?: throw RedstoneToolsException("Invalid stack index $index for world ${world.name}")
        session.setSelectorAndUpdateCUI(player, selector)
        player.info("Selector changed")
    }
}
