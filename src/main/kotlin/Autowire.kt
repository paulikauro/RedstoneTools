package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
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
import java.util.*

@CommandAlias("autowire|aw")
@Description("Get that there redstone automagically!")
@CommandPermission("redstonetools.autowire")
class Autowire(
    private val autos: MutableSet<UUID>,
    private val protocolManager: ProtocolManager
) : BaseCommand() {
    @Default
    fun autowire(player: Player) {
        if (player.uniqueId in autos) {
            autos.remove(player.uniqueId)
            protocolManager.sendResponsePacket(player, "Auto Wire Disabled")
        } else {
            autos.add(player.uniqueId)
            protocolManager.sendResponsePacket(player, "Auto Wire Enabled")
        }
    }

    private fun ProtocolManager.sendResponsePacket(player: Player, message: String) {
        val titlePacket = this.createPacket(PacketType.Play.Server.TITLE)
        titlePacket.titleActions
            .write(0, EnumWrappers.TitleAction.ACTIONBAR)
        titlePacket.chatComponents
            .write(0, WrappedChatComponent.fromText(message))
        this.sendServerPacket(player, titlePacket)
    }
}

class AutoWireListener(
    private val autos: MutableSet<UUID>
) : Listener {
    @EventHandler
    fun onLeaveEvent(event: PlayerQuitEvent) {
        autos.remove(event.player.uniqueId)
    }
    @EventHandler
    fun onAutoWireEvent(event: BlockPlaceEvent) {
        if (event.player.uniqueId !in autos) return
        if (event.player.gameMode != GameMode.CREATIVE) return
        if (!event.block.blockData.material.isSolid) return
        if (event.blockPlaced.type.hasGravity()) return
        val wirePosition = event.blockPlaced.location.add(0.0, 1.0, 0.0)
        if (wirePosition.block.type != Material.AIR) return
        wirePosition.block.type = Material.REDSTONE_WIRE
        val wireData: RedstoneWire = Material.REDSTONE_WIRE.createBlockData() as RedstoneWire
        wireData.allowedFaces.forEach {
            wireData.setFace(it, RedstoneWire.Connection.SIDE)
        }
        wirePosition.block.blockData = wireData
    }
}