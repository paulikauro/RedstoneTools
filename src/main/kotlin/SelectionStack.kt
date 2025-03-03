package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.internal.annotation.Selection
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.limit.PermissiveSelectorLimits
import com.sk89q.worldedit.util.formatting.text.TextComponent
import org.bukkit.entity.Player
import java.util.*

private typealias Stack = MutableList<Pair<BlockVector3, BlockVector3>>

@CommandAlias("/selstack")
@Description("Temporarily save your selection onto a stack")
@CommandPermission("redstonetools.selstack")
class SelectionStack(private val worldEdit: WorldEdit) : BaseCommand() {
    private val stacks = mutableMapOf<UUID, Stack>()

    private fun stackOf(player: WEPlayer): Stack = stacks.computeIfAbsent(player.uniqueId) { mutableListOf() }

    @Default
    @CatchUnknown
    fun help(player: Player) {
        player.sendMessage("Unknown subcommand! Use tab completion or refer to #announcements message")
    }

    @Subcommand("push")
    @Description("Push selection")
    fun push(player: WEPlayer, session: LocalSession, selection: Region) {
        stackOf(player).add(with(selection.boundingBox) { pos1 to pos2 })
        session.getRegionSelector(session.selectionWorld).apply {
            clear()
            explainRegionAdjust(player, session)
        }
        player.printInfo(TextComponent.of("Selection pushed. Selection cleared."))
    }

    @Subcommand("pop")
    @Description("Pop selection")
    fun pop(player: WEPlayer, session: LocalSession) {
        val (pos1, pos2) = stackOf(player).removeLastOrNull() ?: run {
            player.printInfo(TextComponent.of("Your selection stack is empty"))
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
        player.printInfo(TextComponent.of("Your selection stack has been cleared"))
    }

    @Subcommand("show")
    @Description("Show your selection stack")
    fun show(player: WEPlayer) {
        // TODO: click to select?
        player.printInfo(TextComponent.of("Least recent"))
        player.printInfo(TextComponent.of("pos1 / pos2"))
        stackOf(player).forEach { (pos1, pos2) ->
            player.printInfo(TextComponent.of("$pos1 / $pos2"))
        }
        player.printInfo(TextComponent.of("Most recent (top of stack)"))
    }
}
