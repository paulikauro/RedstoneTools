package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

fun PluginScope.createAutoRotate() {
    val autorotate = AutoRotate()
    commandManager.registerCommand(autorotate)
    pluginManager.registerEvents(autorotate, plugin)
}

@CommandAlias("autorotate|ar")
@Description("Automatically rotates specific redstone components when placed.")
@CommandPermission("redstonetools.autorotate")
private class AutoRotate : BaseCommand(), Listener {
    private val enabledPlayers = mutableSetOf<UUID>()
    private val rotatable = setOf(
        Material.REPEATER, Material.COMPARATOR, Material.OBSERVER, Material.PISTON, Material.STICKY_PISTON,
    )

    @Default
    @Description("Toggle autorotate")
    fun toggleAutoRotate(player: Player) {
        player.sendActionBar(
            text(
                if (enabledPlayers.remove(player.uniqueId)) {
                    "Auto Rotate Disabled"
                } else {
                    enabledPlayers.add(player.uniqueId)
                    "Auto Rotate Enabled"
                }
            )
        )
    }

    @CatchUnknown
    fun help(player: Player) {
        player.err("Usage: /autorotate|/ar")
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        enabledPlayers.remove(event.player.uniqueId)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return
        val block = event.block
        val blockData = block.blockData
        if (blockData !is Directional || block.type !in rotatable) return
        blockData.facing = blockData.facing.oppositeFace
        block.blockData = blockData
    }
}
