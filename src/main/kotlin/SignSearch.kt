package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import com.sk89q.jnbt.StringTag
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.function.RegionFunction
import com.sk89q.worldedit.function.RegionMaskingFilter
import com.sk89q.worldedit.function.mask.BlockCategoryMask
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.visitor.RegionVisitor
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import com.sk89q.worldedit.util.formatting.text.serializer.gson.GsonComponentSerializer
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockCategories
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.ceil

val searchResults = HashMap<UUID, MutableList<LocationContainer>>()

@CommandAlias("/signsearch|/ss")
@Description("Search for text of signs within a selection using a regular expression")
@CommandPermission("redstonetools.signsearch")
class SignSearch(private val worldEdit: WorldEdit) : BaseCommand() {
    @Default
    @Syntax("[expression]")
    fun search(
        player: Player,
        arg: String
    ) {
        doSearch(BukkitAdapter.adapt(player), arg)
    }

    @Subcommand("-p")
    @CommandCompletion("@search_page")
    @Syntax("[number]")
    fun page(
        player: Player,
        page: Int
    ) {
        val results = searchResults[player.uniqueId] ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        val paginationBox = LocationsPaginationBox(results, "Search Results", "//signsearch -p %page%")
        val component = try {
            paginationBox.create(page)
        } catch (e: InvalidComponentException) {
            throw RedstoneToolsException("Invalid page number.")
        }
        BukkitAdapter.adapt(player).print(component)
    }

    private fun doSearch(player: WEPlayer, arg: String) {
        val pattern = try {
            Regex(arg)
        } catch (e: Exception) {
            throw RedstoneToolsException("Illegal pattern: " + e.message)
        }
        val session = worldEdit.sessionManager.get(player)
        val selection = try {
            session.getSelection(session.selectionWorld)
        } catch (e: IncompleteRegionException) {
            throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        }
        val matchMap = mutableListOf<LocationContainer>()
        val blockMask = BlockCategoryMask(session.selectionWorld, BlockCategories.SIGNS)
        val regionFunction = RegionFunction { position ->
            val baseBlock = session.selectionWorld.getFullBlock(position)
            val parsedMatch = parseMatch(baseBlock, pattern)
            if (parsedMatch != null) {
                matchMap.add(LocationContainer(position, parsedMatch))
            }
            false
        }
        val regionMaskingFilter = RegionMaskingFilter(blockMask, regionFunction)
        val regionVisitor = RegionVisitor(selection, regionMaskingFilter)
        Operations.complete(regionVisitor)
        if (matchMap.isNotEmpty()) {
            searchResults[player.uniqueId] = matchMap
            page(BukkitAdapter.adapt(player), 1)
        } else {
            searchResults.remove(player.uniqueId)
            player.printInfo(TextComponent.of("No results found."))
        }
    }

    private fun parseMatch(baseBlock: BaseBlock, pattern: Regex): TextComponent? {
        if (!baseBlock.hasNbtData()) {
            return null
        }
        val compoundTag = baseBlock.nbtData!!
        var localMatch: TextComponent? = null
        buildString {
            for (i in 1..4) {
                val textTag = compoundTag.value["Text$i"] as StringTag
                val component = GsonComponentSerializer.INSTANCE.deserialize(textTag.value) as TextComponent
                val currentLine = component.getAllContent()
                append("${currentLine}\n")
                val localMatches = pattern.find(currentLine)
                if (localMatches != null) {
                    localMatch = TextComponent.of("Line $i: ")
                        .color(TextColor.GRAY)
                        .append(currentLine.getHighlightedReplacement(localMatches.groupValues.first()))
                    break
                }
            }
        }.removeSuffix("\n").apply {
            if (localMatch != null) {
                return localMatch
            } else {
                val match = pattern.find(this) ?: return null
                return TextComponent.of("Multi-line match")
                    .color(TextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(getHighlightedReplacement(match.groupValues.first())))
            }
        }
    }
}

class SearchPageCompletionHandler :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
        val player = context.sender as Player
        val locations = searchResults[player.uniqueId] ?: return emptyList()
        return (1..ceil(locations.size / 7f).toInt()).map { it.toString() }.toList()
    }
}
