package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.Component.text
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.data.type.RedstoneWire
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.PluginManager
import java.util.*

fun PluginScope.createAutowire() {
    val autowire = Autowire(pluginManager)
    commandManager.registerCommand(autowire)
    pluginManager.registerEvents(autowire, plugin)
}

@CommandAlias("autowire|aw")
@Description("Get that there redstone automagically!")
@CommandPermission("redstonetools.autowire")
private class Autowire(
    private val pluginManager: PluginManager,
) : BaseCommand(), Listener {
    private val autos = mutableSetOf<UUID>()

    @Default
    @Description("Toggle autowire")
    fun toggleAutowire(player: Player) {
        player.sendActionBar(
            text(
                if (autos.remove(player.uniqueId)) {
                    "Auto wire Disabled"
                } else {
                    autos.add(player.uniqueId)
                    "Auto wire Enabled"
                }
            )
        )
    }

    @CatchUnknown
    fun help(player: Player) {
        player.err("Usage /autowire|/aw")
    }

    @EventHandler
    fun onLeaveEvent(event: PlayerQuitEvent) {
        autos.remove(event.player.uniqueId)
    }

    @EventHandler(ignoreCancelled = true)
    fun onAutoWireEvent(event: BlockPlaceEvent) {
        if (event.player.uniqueId !in autos
            || event.player.gameMode != GameMode.CREATIVE
            || !event.block.blockData.material.isSolid
            || event.blockPlaced.type.hasGravity()
        ) return
        val wirePosition = event.blockPlaced.location.add(0.0, 1.0, 0.0)
        if (wirePosition.block.type != Material.AIR) return
        val airState = wirePosition.block.state
        wirePosition.block.type = Material.REDSTONE_WIRE
        val blockPlaceEvent = BlockPlaceEvent(
            wirePosition.block,
            airState,
            event.blockPlaced,
            ItemStack(Material.REDSTONE),
            event.player,
            true,
            event.hand
        )
        pluginManager.callEvent(blockPlaceEvent)
        if (blockPlaceEvent.isCancelled) {
            wirePosition.block.type = Material.AIR
            return
        }
        wirePosition.block.type = Material.REDSTONE_WIRE
        val wireData = Material.REDSTONE_WIRE.createBlockData() as RedstoneWire
        wireData.allowedFaces.forEach {
            wireData.setFace(it, RedstoneWire.Connection.SIDE)
        }
        wirePosition.block.blockData = wireData
    }
}
