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
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.serializer.gson.GsonComponentSerializer
import com.sk89q.worldedit.world.block.BlockCategories
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.ceil

val searchResults = HashMap<UUID, MutableList<BlockVector3>>()

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
        val locations = searchResults[player.uniqueId] ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        val paginationBox = LocationsPaginationBox(locations, "Search Results", "//signsearch -p %page%")
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
        val matches = mutableListOf<BlockVector3>()
        val blockMask = BlockCategoryMask(session.selectionWorld, BlockCategories.SIGNS)
        val regionFunction = RegionFunction { position ->
            val baseBlock = session.selectionWorld.getFullBlock(position)
            if (baseBlock.hasNbtData()) {
                val compoundTag = baseBlock.nbtData!!
                val content = buildString {
                    for (i in 1..4) {
                        val textTag = compoundTag.value["Text$i"] as StringTag
                        val component = GsonComponentSerializer.INSTANCE.deserialize(textTag.value) as TextComponent
                        append(component.getAllContent())
                    }
                }
                if (content.contains(pattern)) {
                    matches.add(position)
                }
            }
            false
        }
        val regionMaskingFilter = RegionMaskingFilter(blockMask, regionFunction)
        val regionVisitor = RegionVisitor(selection, regionMaskingFilter)
        Operations.complete(regionVisitor)
        if (matches.isNotEmpty()) {
            searchResults[player.uniqueId] = matches
            page(BukkitAdapter.adapt(player), 1)
        } else {
            searchResults.remove(player.uniqueId)
            player.printInfo(TextComponent.of("No results found."))
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
