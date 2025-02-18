package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

@CommandAlias("autorotate|ar")
@Description("Automatically rotates specific redstone components when placed.")
@CommandPermission("redstonetools.autorotate")
class AutoRotate : BaseCommand(), Listener {
    private val enabledPlayers = mutableMapOf<UUID, String>()

    @Default
    @CommandCompletion("horizontal|vertical")
    fun toggleAutoRotate(player: Player, @Optional mode: String?) {
        val rotationMode = mode ?: "horizontal" // Default mode is horizontal
        val key = player.uniqueId

        val message = if (enabledPlayers.remove(key) != null) {
            "Auto Rotate ($rotationMode) Disabled"
        } else {
            enabledPlayers[key] = rotationMode
            "Auto Rotate ($rotationMode) Enabled"
        }

        player.sendActionBar(message)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        enabledPlayers.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val rotationMode = enabledPlayers[player.uniqueId] ?: return
        val block = event.block
        val blockData = block.blockData

        val horizontalRotatable = setOf(
            Material.REPEATER,
            Material.COMPARATOR,
            Material.OBSERVER,
            Material.PISTON,
            Material.STICKY_PISTON
        )

        val verticalRotatable = setOf(
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.OBSERVER
        )

        if (block.type in horizontalRotatable && blockData is Directional && rotationMode == "horizontal") {
            blockData.facing = blockData.facing.rotateHorizontal()
            block.blockData = blockData
        } else if (block.type in verticalRotatable && blockData is Directional && rotationMode == "vertical") {
            blockData.facing = blockData.facing.rotateVertical()
            block.blockData = blockData
        }
    }

    private fun BlockFace.rotateHorizontal(): BlockFace = when (this) {
        BlockFace.NORTH -> BlockFace.SOUTH
        BlockFace.SOUTH -> BlockFace.NORTH
        BlockFace.EAST -> BlockFace.WEST
        BlockFace.WEST -> BlockFace.EAST
        else -> this
    }

    private fun BlockFace.rotateVertical(): BlockFace = when (this) {
        BlockFace.UP -> BlockFace.DOWN
        BlockFace.DOWN -> BlockFace.UP
        else -> this
    }
}
