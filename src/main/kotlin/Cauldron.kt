package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*

@CommandAlias("cauldron")
@Description("Toggles cauldron water level adjustment mode.")
@CommandPermission("redstonetools.cauldron")
class Cauldron() : BaseCommand(), Listener {
    private val enabledPlayers = mutableSetOf<UUID>()

    @Default
    fun toggleCauldronMode(player: Player) {
        val message = if (enabledPlayers.remove(player.uniqueId)) {
            "Cauldron Mode Disabled"
        } else {
            enabledPlayers.add(player.uniqueId)
            "Cauldron Mode Enabled"
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
    fun onCauldronInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return
        if (event.hand != EquipmentSlot.HAND) return
        if (event.item != null) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.CAULDRON && block.type != Material.WATER_CAULDRON) return

        event.isCancelled = true
        adjustCauldron(block, player)
    }

    private fun adjustCauldron(block: Block, player: Player) {
        if (block.type == Material.CAULDRON) {
            block.type = Material.WATER_CAULDRON
            val newData = block.blockData as Levelled
            newData.level = 1
            updateCauldron(block, newData, player, Sound.ITEM_BUCKET_FILL, "Cauldron water level: 1")
            return
        }

        val cauldronData = block.blockData as? Levelled ?: return
        if (cauldronData.level < cauldronData.maximumLevel) {
            cauldronData.level++
            updateCauldron(block, cauldronData, player, Sound.ITEM_BUCKET_FILL, "Cauldron water level: ${cauldronData.level}")
        } else {
            block.type = Material.CAULDRON
            updateCauldron(block, null, player, Sound.ITEM_BUCKET_EMPTY, "Cauldron emptied")
        }
    }

    private fun updateCauldron(block: Block, newData: Levelled?, player: Player, sound: Sound, message: String) {
        if (newData != null) block.blockData = newData
        block.state.update(true, false)
        block.world.playSound(block.location, sound, 1.0f, 1.0f)
        player.sendActionBar(message)
    }
}
