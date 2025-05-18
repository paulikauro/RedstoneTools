package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.function.RegionFunction
import com.sk89q.worldedit.function.RegionMaskingFilter
import com.sk89q.worldedit.function.mask.BlockCategoryMask
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.visitor.RegionVisitor
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockCategories
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.entity.Player
import org.enginehub.linbus.tree.LinTagType.compoundTag
import org.enginehub.linbus.tree.LinTagType.stringTag
import java.util.*
import kotlin.math.ceil
import net.kyori.adventure.text.TextComponent as ATextComponent

val searchResults = HashMap<UUID, MutableList<LocationContainer>>()

@CommandAlias("/signsearch|/ss")
@Description("Search for text of signs within a selection using a regular expression")
@CommandPermission("redstonetools.signsearch")
class SignSearch() : BaseCommand() {
    @Default
    @Syntax("[expression]")
    fun search(
        player: WEPlayer,
        session: LocalSession,
        selection: Region,
        arg: String
    ) {
        val pattern = try {
            Pattern.compile(arg)
        } catch (e: PatternSyntaxException) {
            throw RedstoneToolsException("Illegal pattern: " + e.message)
        }
        val matches = mutableListOf<LocationContainer>()
        // it should not be null if we got this far
        val world = selection.world!!
        val blockMask = BlockCategoryMask(world, BlockCategories.ALL_SIGNS)
        val regionFunction = RegionFunction { position ->
            val baseBlock = world.getFullBlock(position)
            val match = parseMatch(baseBlock, pattern)
            if (match != null) {
                matches.add(LocationContainer(position, match))
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

    @Subcommand("-p")
    @CommandCompletion("@search_page")
    @Syntax("[number]")
    fun page(
        player: Player,
        page: Int
    ) {
        val results = searchResults[player.uniqueId] ?: throw RedstoneToolsException("Use //signsearch to get results")
        val paginationBox = LocationsPaginationBox(results, "Search Results", "//signsearch -p %page%")
        val component = try {
            paginationBox.create(page)
        } catch (_: InvalidComponentException) {
            throw RedstoneToolsException("Invalid page number.")
        }
        BukkitAdapter.adapt(player).print(component)
    }

    private fun parseMatch(baseBlock: BaseBlock, pattern: Pattern): TextComponent? {
        val nbt = baseBlock.nbt ?: return null
        fun messages(side: String) =
            nbt.getTag("${side}_text", compoundTag()).getListTag("messages", stringTag()).value()

        // TODO: front/back in results
        return (messages("front") + messages("back"))
            .map { (GsonComponentSerializer.gson().deserialize(it.value()) as ATextComponent).content() }
            .mapIndexedNotNull { index, line ->
                line
                    .findFirstMatch(pattern)
                    ?.let {
                        TextComponent.of("Line ${index + 1}: ")
                            .color(TextColor.GRAY)
                            .append(line.withHighlightedReplacement(it.text))
                    }
            }
            .ifEmpty { null }
            ?.let { matchComponents -> TextComponent.join(TextComponent.newline(), matchComponents) }
        // TODO: multiline matches
//            .ifEmpty { lines.joinToString("\n").findAll(pattern) }
    }
}

private fun String.withHighlightedReplacement(replacement: String): TextComponent =
    TextComponent.of(this.substringBefore(replacement))
        .color(TextColor.WHITE)
        .append(
            TextComponent.of(replacement)
                .color(TextColor.YELLOW)
        )
        .append(
            TextComponent.of(this.substringAfter(replacement))
                .color(TextColor.WHITE)
        )

private data class Match(val text: String, val start: Int, val end: Int)

private fun String.findFirstMatch(pattern: Pattern): Match? {
    val matcher = pattern.matcher(this)
    if (!matcher.find()) return null
    return Match(matcher.group(), matcher.start(), matcher.end())
}

class SearchPageCompletionHandler :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
        val player = context.sender as Player
        val locations = searchResults[player.uniqueId] ?: return emptyList()
        return (1..ceil(locations.size / 7f).toInt()).map { it.toString() }.toList()
    }
}
