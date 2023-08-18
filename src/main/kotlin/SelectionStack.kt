package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.selector.limit.PermissiveSelectorLimits
import org.bukkit.entity.Player
import java.util.*

private typealias Stack = MutableList<Pair<BlockVector3, BlockVector3>>

@CommandAlias("/selstack")
@Description("Temporarily save your selection onto a stack")
@CommandPermission("redstonetools.selstack")
class SelectionStack(private val worldEdit: WorldEdit) : BaseCommand() {
    private val stacks = mutableMapOf<UUID, Stack>()

    private fun stackOf(player: Player): Stack = stacks.computeIfAbsent(player.uniqueId) { mutableListOf() }

    @Default
    @CatchUnknown
    fun help(player: Player) {
        player.sendMessage("Unknown subcommand! Use tab completion or refer to #announcements message")
    }

    @Subcommand("push")
    @Description("Push selection")
    fun push(player: Player) {
        // todo: refactor
        val wePlayer = BukkitAdapter.adapt(player)
        val session = worldEdit.sessionManager.get(wePlayer)
        val selection = try {
            session.getSelection(session.selectionWorld ?: throw IncompleteRegionException())
        } catch (exception: IncompleteRegionException) {
            player.sendMessage("You do not have a selection")
            return
        }
        stackOf(player).add(with(selection.boundingBox) { pos1 to pos2 })
        session.getRegionSelector(session.selectionWorld).apply {
            clear()
            explainRegionAdjust(wePlayer, session)
        }
        player.sendMessage("Selection pushed. Selection cleared.")
    }

    @Subcommand("pop")
    @Description("Pop selection")
    fun pop(player: Player) {
        val wePlayer = BukkitAdapter.adapt(player)
        val (pos1, pos2) = stackOf(player).removeLastOrNull() ?: run {
            player.sendMessage("Your selection stack is empty")
            return
        }
        val session = worldEdit.sessionManager.get(wePlayer)
        session.getRegionSelector(BukkitAdapter.adapt(player.world)).apply {
            clear()
            selectPrimary(pos1, PermissiveSelectorLimits.getInstance())
            selectSecondary(pos2, PermissiveSelectorLimits.getInstance())
            explainRegionAdjust(wePlayer, session)
        }
    }

    @Subcommand("clear")
    @Description("Clear your selection stack")
    fun clear(player: Player) {
        stackOf(player).clear()
        player.sendMessage("Your selection stack has been cleared")
    }

    @Subcommand("show")
    @Description("Show your selection stack")
    fun show(player: Player) {
        // TODO: click to select?
        player.sendMessage("Least recent")
        player.sendMessage("pos1 / pos2")
        stackOf(player).forEach { (pos1, pos2) ->
            player.sendMessage("$pos1 / $pos2")
        }
        player.sendMessage("Most recent (top of stack)")
    }
}
