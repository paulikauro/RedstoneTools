package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extension.factory.MaskFactory
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.function.RegionFunction
import com.sk89q.worldedit.function.RegionMaskingFilter
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.visitor.RegionVisitor
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.text.TextComponent
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.ceil

val findResults = HashMap<UUID, MutableList<LocationContainer>>()

@CommandAlias("/find")
@Description("Find some shid in selecton")
@CommandPermission("redstonetools.find")
class Find(private val worldEdit: WorldEdit) : BaseCommand() {
    @Default
    @CommandCompletion("@we_mask")
    @Syntax("[material]")
    fun find(
        player: Player,
        arg: String
    ) {
        doFind(BukkitAdapter.adapt(player), arg)
    }

    @Subcommand("-p")
    @CommandCompletion("@find_page")
    @Syntax("[number]")
    fun page(
        player: Player,
        page: Int
    ) {
        val locations = findResults[player.uniqueId] ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        val paginationBox = LocationsPaginationBox(locations, "Find Results", "//find -p %page%")
        val component = try {
            paginationBox.create(page)
        } catch (e: InvalidComponentException) {
            throw RedstoneToolsException("Invalid page number.")
        }
        BukkitAdapter.adapt(player).print(component)
    }

    private fun doFind(player: WEPlayer, arg: String) {
        val session = worldEdit.sessionManager.get(player)
        val selection = try {
            session.getSelection(session.selectionWorld)
        } catch (e: IncompleteRegionException) {
            throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        }
        val locations = mutableListOf<LocationContainer>()
        val maskFactory = MaskFactory(worldEdit)
        val parserContext = ParserContext().apply {
            extent = session.selectionWorld
        }
        val blockMask = maskFactory.parseFromInput(arg, parserContext)
        val regionFunction = RegionFunction { position ->
            locations.add(LocationContainer(position, TextComponent.of(position.toString())))
            false
        }
        val regionMaskingFilter = RegionMaskingFilter(blockMask, regionFunction)
        val regionVisitor = RegionVisitor(selection, regionMaskingFilter)
        Operations.complete(regionVisitor)
        if (locations.isNotEmpty()) {
            findResults[player.uniqueId] = locations
            page(BukkitAdapter.adapt(player), 1)
        } else {
            findResults.remove(player.uniqueId)
            player.printInfo(TextComponent.of("No results found."))
        }
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
