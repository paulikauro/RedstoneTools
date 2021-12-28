package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
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
class LiveStack(private val plugin: Plugin, private val worldEdit: WorldEdit) : BaseCommand(), Listener {
    private val gonnaLiveStack = mutableMapOf<UUID, State>()

    sealed interface State {
        data class SelectingRoot(val blocks: List<BlockVector3>) : State
        data class Enabled(val displacements: List<BlockVector3>) : State
    }

    @Default
    fun livestack(player: Player) {
        if (player.uniqueId in gonnaLiveStack) {
            gonnaLiveStack.remove(player.uniqueId)
            "Live Stack Disabled"
        } else {
            val wePlayer = BukkitAdapter.adapt(player)
            // TODO: factor this out (shared with RStack)
            val session = worldEdit.sessionManager.get(wePlayer)
            val selectionWorld = session.selectionWorld ?: throw ConditionFailedException("no selection world !?")
            val selection = try {
                // TODO: is try catch needed?
                session.getSelection(selectionWorld)
            } catch (e: IncompleteRegionException) {
                throw ConditionFailedException("You do not have a selection!")
            }
            val blocks = selection.filter { !selectionWorld.getBlock(it).blockType.material.isAir }
            gonnaLiveStack[player.uniqueId] = State.SelectingRoot(blocks)
            "Click to select root block"
        }.let { player.sendMessage(it) }
    }

    @EventHandler
    fun onLiveStackEvent(event: BlockPlaceEvent) {
        val (displacements) = gonnaLiveStack[event.player.uniqueId] as? State.Enabled ?: return
        doLiveStack(event.block, displacements)
    }

    @EventHandler
    fun onLiveStackEvent(event: BlockBreakEvent) {
        when (val state = gonnaLiveStack[event.player.uniqueId]) {
            is State.SelectingRoot -> {
                val root = event.block.location.toBlockVector3()
                gonnaLiveStack[event.player.uniqueId] = State.Enabled(
                    displacements = state.blocks.map { it.subtract(root) }.filter { it != BlockVector3.ZERO }
                )
                event.isCancelled = true
                event.player.sendMessage("root block selected")
            }
            is State.Enabled -> {
                doLiveStack(event.block, state.displacements)
            }
            else -> Unit
        }
    }

    @EventHandler
    fun onLiveStackEvent(event: PlayerInteractEvent) {
        val (displacements) = gonnaLiveStack[event.player.uniqueId] as? State.Enabled ?: return
        event.clickedBlock?.let {
            doLiveStack(it, displacements)
        }
    }

    private fun doLiveStack(
        block: Block,
        displacements: List<BlockVector3>,
    ) = Bukkit.getScheduler().runTask(plugin, Runnable {
        displacements.forEach {
            val newBlock = block.location.add(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()).block
            newBlock.blockData = block.blockData
        }
    })
}
