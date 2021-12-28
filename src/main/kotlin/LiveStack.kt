package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitPlayer
import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import java.util.*

@CommandAlias("/livestack|/ls")
@Description("Redstone live stacking command")
@CommandPermission("redstonetools.livestack")
class LiveStack(private val worldEdit: WorldEdit, private val plugin: Plugin) : BaseCommand(), Listener {
    private val gonnaLiveStack = mutableSetOf<UUID>()

    @Default
    fun livestack(player: Player) {
        if (player.uniqueId in gonnaLiveStack) {
            gonnaLiveStack.remove(player.uniqueId)
            "Live Stack Disabled"
        } else {
            gonnaLiveStack.add(player.uniqueId)
            "Live Stack Enabled"
        }.let { player.sendMessage(it) }
    }

    @EventHandler
    fun onLiveStackEvent(event: BlockPlaceEvent) {
        if (event.player.uniqueId !in gonnaLiveStack) return
        doLiveStack(BukkitAdapter.adapt(event.player), event.block)
    }

    @EventHandler
    fun onLiveStackEvent(event: BlockBreakEvent) {
        if (event.player.uniqueId !in gonnaLiveStack) return
        doLiveStack(BukkitAdapter.adapt(event.player), event.block)
    }

    @EventHandler
    fun onLiveStackEvent(event: PlayerInteractEvent) {
        if (event.player.uniqueId !in gonnaLiveStack) return
        event.clickedBlock?.let {
            doLiveStack(BukkitAdapter.adapt(event.player), it)
        }
    }

    private fun doLiveStack(
        player: BukkitPlayer?,
        block: Block,
    ) = Bukkit.getScheduler().runTask(plugin, Runnable {
        val session = worldEdit.sessionManager.get(player)
        val displacements = arrayOf(
            BlockVector3.at(-2, -2, 0),
            BlockVector3.at(-2, 2, 0),
            BlockVector3.at(-4, 0, 0),
        )
        session.createEditSession(player).use { editSession ->
            displacements.forEach {
                editSession.setBlock(
                    block.location.toBlockVector3().add(it),
                    BukkitAdapter.adapt(block.blockData)
                )
            }
            session.remember(editSession)
        }
    })
}
