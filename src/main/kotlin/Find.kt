package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.function.RegionFunction
import com.sk89q.worldedit.function.RegionMaskingFilter
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.visitor.RegionVisitor
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.text.TextComponent
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.ceil

val findResults = HashMap<UUID, MutableList<LocationContainer>>()

@CommandAlias("/find")
@Description("Find blocks matching a mask in your selection")
@CommandPermission("redstonetools.find")
class Find : BaseCommand() {
    @Default
    @Syntax("[mask]")
    fun find(
        player: WEPlayer,
        mask: Mask,
        selection: Region,
    ) {
        // TODO: this gives you "no match for asdf" error if you give it an invalid mask
        //  it should be something nicer
        val locations = mutableListOf<LocationContainer>()
        val regionFunction = RegionFunction { position ->
            locations.add(LocationContainer(position, TextComponent.of(position.toString())))
            false
        }
        val regionMaskingFilter = RegionMaskingFilter(mask, regionFunction)
        val regionVisitor = RegionVisitor(selection, regionMaskingFilter)
        Operations.complete(regionVisitor)
        if (locations.isNotEmpty()) {
            findResults[player.uniqueId] = locations
            page(player, 1)
        } else {
            findResults.remove(player.uniqueId)
            player.info("No results found")
        }
    }

    @Subcommand("-p")
    @CommandCompletion("@find_page")
    @Syntax("[number]")
    fun page(
        player: WEPlayer,
        page: Int,
    ) {
        val locations = findResults[player.uniqueId] ?: throw RedstoneToolsException("Use //find to get results")
        val paginationBox = LocationsPaginationBox(locations, "Find Results", "//find -p %page%")
        val component = try {
            paginationBox.create(page)
        } catch (_: InvalidComponentException) {
            throw RedstoneToolsException("Invalid page number.")
        }
        player.print(component)
    }
}

class FindPageCompletionHandler :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
        val player = context.sender as Player
        val locations = findResults[player.uniqueId] ?: return emptyList()
        return (1..ceil(locations.size / 7f).toInt()).map { it.toString() }.toList()
    }
}
