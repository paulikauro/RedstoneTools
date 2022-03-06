package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Description
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import kotlin.collections.ArrayDeque

@CommandAlias("destroy")
@Description("Destroy stuff. CAREFUL!")
@CommandPermission("redstonetools.destroy")
class Destroy(private val worldEdit: WorldEdit) : BaseCommand(), Listener {
    private val gonnaDestroy = mutableSetOf<UUID>()

    @Default
    fun destroy(player: Player) {
        player.sendMessage("going to destroy the next thing you click!!!")
        gonnaDestroy.add(player.uniqueId)
    }

    @EventHandler
    fun onLeaveEvent(event: PlayerQuitEvent) {
        gonnaDestroy.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onKickEvent(event: PlayerKickEvent) {
        gonnaDestroy.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!gonnaDestroy.remove(event.player.uniqueId)) return
        event.isCancelled = true
        val blocksToDelete = search(event.block)
        val player = BukkitAdapter.adapt(event.player)
        val session = worldEdit.sessionManager.get(player)
        var affected = 0
        val air = BlockTypes.AIR!!.defaultState
        session.createEditSession(player).use { editSession ->
            blocksToDelete.forEach {
                val success = editSession.setBlock(it, air)
                if (success) affected++
            }
            session.remember(editSession)
            // TODO: flush block bag?
        }
        player.printInfo(TextComponent.of("Operation completed, $affected blocks affected"))
    }

    private fun search(first: Block): MutableSet<BlockVector3> {
        val world = first.world
        val found = mutableSetOf<BlockVector3>()
        val visited = mutableSetOf<BlockVector3>()
        val queue = ArrayDeque<BlockVector3>()
        queue.add(first.location.toBlockVector3())
        val offsets = arrayOf(
            BlockVector3.UNIT_X,
            BlockVector3.UNIT_Y,
            BlockVector3.UNIT_Z,
            BlockVector3.UNIT_MINUS_X,
            BlockVector3.UNIT_MINUS_Y,
            BlockVector3.UNIT_MINUS_Z,
        )
        // TODO: parameterize
        while (queue.isNotEmpty() && found.size < 128) {
            val pos = queue.removeFirst()
            if (pos in visited) continue
            visited.add(pos)
            val block = world.getBlockAt(pos.blockX, pos.blockY, pos.blockZ)
            // ignore air
            if (block.isEmpty) {
                continue
            }
            found.add(pos)
            offsets.forEach {
                val newPos = pos.add(it)
                // eeh checking twice because reasons
                if (newPos !in visited) {
                    queue.addLast(newPos)
                }
            }
        }
        return found
    }
}
