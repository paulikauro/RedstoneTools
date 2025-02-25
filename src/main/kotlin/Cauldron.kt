package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*

@CommandAlias("cauldron")
@Description("Toggles cauldron water level adjustment mode.")
@CommandPermission("redstonetools.cauldron")
class Cauldron : BaseCommand(), Listener {
    private val enabledPlayers = mutableSetOf<UUID>()

    @Default
    fun toggleCauldronMode(player: Player) {
        player.sendActionBar(
            if (enabledPlayers.remove(player.uniqueId)) {
                "Cauldron Mode Disabled"
            } else {
                enabledPlayers.add(player.uniqueId)
                "Cauldron Mode Enabled"
            }
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        enabledPlayers.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onCauldronInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return
        if (event.hand != EquipmentSlot.HAND) return
        if (event.item != null) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.CAULDRON && block.type != Material.WATER_CAULDRON) return
        event.isCancelled = true
        val level = updateCauldron(block)
        block.state.update(true, false)
        val sound = if (level == 0) Sound.ITEM_BUCKET_EMPTY else Sound.ITEM_BUCKET_FILL
        block.world.playSound(block.location, sound, 1.0f, 1.0f)
        player.sendActionBar("Cauldron water level: $level")
    }

    private fun updateCauldron(block: Block): Int {
        if (block.type == Material.CAULDRON) {
            block.type = Material.WATER_CAULDRON
            val newData = block.blockData as Levelled
            newData.level = 1
            block.blockData = newData
            return 1
        }
        val cauldronData = block.blockData as Levelled
        if (cauldronData.level < cauldronData.maximumLevel) {
            cauldronData.level++
            block.blockData = cauldronData
            return cauldronData.level
        } else {
            block.type = Material.CAULDRON
            return 0
        }
    }
}
