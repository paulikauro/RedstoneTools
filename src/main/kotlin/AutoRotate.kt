package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Orientable
import org.bukkit.block.data.type.Comparator
import org.bukkit.block.data.type.Observer
import org.bukkit.block.data.type.Repeater
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.*

@CommandAlias("autorotate|ar")
@Description("Automatically rotates specific redstone components by 180° when placed.")
@CommandPermission("redstonetools.autorotate")
class AutoRotate(private val plugin: Plugin) : BaseCommand(), Listener {
    private val enabledPlayers = mutableSetOf<UUID>()

    @Default
    fun toggleAutoRotate(player: Player) {
        val message = if (enabledPlayers.remove(player.uniqueId)) {
            "Auto Rotate Disabled"
        } else {
            enabledPlayers.add(player.uniqueId)
            "Auto Rotate Enabled"
        }
        player.sendActionBar(message)
    }

    private fun Player.sendActionBar(message: String) {
        spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message))
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        enabledPlayers.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return

        val block = event.block
        val blockData = block.blockData

        when (blockData) {
            is Repeater, is Comparator, is Observer -> {
                if (blockData is Directional) {
                    blockData.facing = blockData.facing.rotate180()
                    block.blockData = blockData
                }
            }
            is Orientable -> {
                blockData.axis = blockData.axis.rotate180()
                block.blockData = blockData
            }
            is Directional -> {
                if (block.type == Material.PISTON || block.type == Material.STICKY_PISTON) {
                    blockData.facing = blockData.facing.rotate180()
                    block.blockData = blockData
                }
            }
        }
    }

    private fun BlockFace.rotate180(): BlockFace {
        return when (this) {
            BlockFace.NORTH -> BlockFace.SOUTH
            BlockFace.SOUTH -> BlockFace.NORTH
            BlockFace.EAST -> BlockFace.WEST
            BlockFace.WEST -> BlockFace.EAST
            BlockFace.UP, BlockFace.DOWN -> this
            else -> this
        }
    }

    private fun Axis.rotate180(): Axis {
        return when (this) {
            Axis.X -> Axis.X
            Axis.Z -> Axis.Z
            Axis.Y -> Axis.Y
        }
    }
}