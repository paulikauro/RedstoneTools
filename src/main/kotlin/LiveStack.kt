package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.function.mask.ExistingBlockMask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.text.TextComponent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.block.Block
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
    fun livestack(player: WEPlayer, selection: Region) {
        if (player.uniqueId in gonnaLiveStack) {
            gonnaLiveStack.remove(player.uniqueId)
            "Live Stack Disabled"
        } else {
            // world is always defined when we get the selection from ACF
            val mask = ExistingBlockMask(selection.world!!)
            val blocks = selection.filterNot(mask::test)
            gonnaLiveStack[player.uniqueId] = State.SelectingRoot(blocks)
            "Click to select root block"
        }.let { player.printInfo(TextComponent.of(it)) }
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
                event.player.sendMessage(Component.text("root block selected"))
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
