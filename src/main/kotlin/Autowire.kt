package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Description
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.WrappedChatComponent
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

@CommandAlias("autowire|aw")
@Description("Get that there redstone automagically!")
@CommandPermission("redstonetools.autowire")
class Autowire(
    private val protocolManager: ProtocolManager,
    private val pluginManager: PluginManager,
) : BaseCommand(), Listener {
    private val autos = mutableSetOf<UUID>()

    @Default
    fun toggleAutowire(player: Player) {
        if (player.uniqueId in autos) {
            autos.remove(player.uniqueId)
            "Auto Wire Disabled"
        } else {
            autos.add(player.uniqueId)
            "Auto Wire Enabled"
        }.let { player.sendActionBarTitle(it) }
    }

    private fun Player.sendActionBarTitle(message: String) {
        val titlePacket = protocolManager.createPacket(PacketType.Play.Server.TITLE).apply {
            titleActions.write(0, EnumWrappers.TitleAction.ACTIONBAR)
            chatComponents.write(0, WrappedChatComponent.fromText(message))
        }
        protocolManager.sendServerPacket(this, titlePacket)
    }

    @EventHandler
    fun onLeaveEvent(event: PlayerQuitEvent) {
        autos.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onAutoWireEvent(event: BlockPlaceEvent) {
        with(event) {
            arrayOf(
                player.uniqueId !in autos,
                player.gameMode != GameMode.CREATIVE,
                !block.blockData.material.isSolid,
                blockPlaced.type.hasGravity(),
            ).any().ifTrue { return }
        }
        val wirePosition = event.blockPlaced.location.add(0.0, 1.0, 0.0)
        if (wirePosition.block.type != Material.AIR) return
        val airState = wirePosition.block.state
        val blockPlaceEvent = BlockPlaceEvent(
            wirePosition.block,
            airState,
            event.blockPlaced,
            ItemStack(Material.REDSTONE_WIRE),
            event.player,
            true,
            event.hand
        )
        pluginManager.callEvent(blockPlaceEvent)
        if (blockPlaceEvent.isCancelled) return
        wirePosition.block.type = Material.REDSTONE_WIRE
        val wireData = Material.REDSTONE_WIRE.createBlockData() as RedstoneWire
        wireData.allowedFaces.forEach {
            wireData.setFace(it, RedstoneWire.Connection.SIDE)
        }
        wirePosition.block.blockData = wireData
    }
}

private inline fun Boolean.ifTrue(block: () -> Unit) {
    if (this) block()
}