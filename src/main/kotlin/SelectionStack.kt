package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.limit.PermissiveSelectorLimits
import java.util.*

private typealias Stack = MutableList<Pair<BlockVector3, BlockVector3>>

@CommandAlias("/selstack")
@Description("Temporarily save your selection onto a stack")
@CommandPermission("redstonetools.selstack")
class SelectionStack : BaseCommand() {
    private val stacks = mutableMapOf<UUID, Stack>()

    private fun stackOf(player: WEPlayer): Stack = stacks.computeIfAbsent(player.uniqueId) { mutableListOf() }

    @HelpCommand
    fun help(help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("push")
    @Description("Push selection")
    fun push(player: WEPlayer, session: LocalSession, selection: Region) {
        stackOf(player).add(with(selection.boundingBox) { pos1 to pos2 })
        session.getRegionSelector(session.selectionWorld).apply {
            clear()
            explainRegionAdjust(player, session)
        }
        player.info("Selection pushed. Selection cleared.")
    }

    @Subcommand("pop")
    @Description("Pop selection")
    fun pop(player: WEPlayer, session: LocalSession) {
        val (pos1, pos2) = stackOf(player).removeLastOrNull() ?: run {
            player.info("Your selection stack is empty")
            return
        }
        session.getRegionSelector(player.world).apply {
            clear()
            selectPrimary(pos1, PermissiveSelectorLimits.getInstance())
            selectSecondary(pos2, PermissiveSelectorLimits.getInstance())
            explainRegionAdjust(player, session)
        }
    }

    @Subcommand("clear")
    @Description("Clear your selection stack")
    fun clear(player: WEPlayer) {
        stackOf(player).clear()
        player.info("Your selection stack has been cleared")
    }

    @Subcommand("show")
    @Description("Show your selection stack")
    fun show(player: WEPlayer) {
        // TODO: click to select?
        player.info("Least recent")
        player.info("pos1 / pos2")
        stackOf(player).forEach { (pos1, pos2) ->
            player.info("$pos1 / $pos2")
        }
        player.info("Most recent (top of stack)")
    }
}
