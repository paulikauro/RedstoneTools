package io.github.paulikauro.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.google.re2j.Matcher
import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException
import com.sk89q.jnbt.CompoundTag
import com.sk89q.jnbt.ListTag
import com.sk89q.jnbt.StringTag
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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import java.util.*

fun PluginScope.createSignSearch() {
    val searchResults = HashMap<UUID, List<LocationContainer>>()
    commandManager.apply {
        commandCompletions.registerCompletion(COMPLETION_SEARCH_PAGE, PageCompletionHandler(searchResults))
        registerCommand(SignSearch(searchResults))
    }
}

private const val COMPLETION_SEARCH_PAGE = "search_page"

@CommandAlias("/signsearch|/ss")
@Description("Search for text of signs within a selection using a regular expression")
@CommandPermission("redstonetools.signsearch")
private class SignSearch(private val searchResults: MutableMap<UUID, List<LocationContainer>>) : BaseCommand() {
    @Default
    @Syntax("[regex]")
    fun search(
        player: WEPlayer,
        selection: Region,
        arg: String,
    ) {
        // TODO:
        //  - //ss without selection gives you "plz select first"
        //  - //ss with selection gives you usage
        val pattern = try {
            Pattern.compile(arg)
        } catch (e: PatternSyntaxException) {
            throw RedstoneToolsException("Illegal pattern: " + e.message)
        }
        val matches = mutableListOf<LocationContainer>()
        // selection's world is never null when given from the command context
        val world = selection.world!!
        // BlockCategories.ALL_SIGNS when WorldEdit update
        val blockMask = BlockCategoryMask(world, BlockCategories.get("minecraft:all_signs"))
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
            page(player.bukkit(), 1)
        } else {
            searchResults.remove(player.uniqueId)
            player.info("No results found.")
        }
    }

    @Subcommand("-p")
    @CommandCompletion("@$COMPLETION_SEARCH_PAGE")
    @Syntax("[number]")
    fun page(
        player: Player,
        page: Int,
    ) {
        val results = searchResults[player.uniqueId] ?: throw RedstoneToolsException("Use //signsearch to get results")
        val paginationBox = LocationsPaginationBox(results, "Search Results", "//signsearch -p %page%")
        val component = try {
            paginationBox.create(page)
        } catch (_: InvalidComponentException) {
            throw RedstoneToolsException("Invalid page number.")
        }
        player.we().print(component)
    }

    private fun parseMatch(baseBlock: BaseBlock, pattern: Pattern): TextComponent? {
        val compoundTag = baseBlock.nbtData ?: return null
        val front = (compoundTag.value["front_text"] as CompoundTag).value["messages"] as ListTag
        val back = (compoundTag.value["back_text"] as CompoundTag).value["messages"] as ListTag
        val messages = front.value + back.value
        val lines = messages.map { tag -> json2plain((tag as StringTag).value) }

        return lines
            .mapIndexedNotNull { index, line ->
                val (didMatch, parts) = line.splitMap(pattern, noMatch = TextComponent::of) { matcher ->
                    val m = matcher.group()
                    if (m.isEmpty()) {
                        TextComponent.of("|").color(TextColor.RED)
                    } else {
                        TextComponent.of(matcher.group()).color(TextColor.YELLOW)
                    }
                }
                if (didMatch) {
                    TextComponent.of("Line ${index + 1}: ")
                        .color(TextColor.GRAY)
                        .append(TextComponent.join(TextComponent.empty(), parts).colorIfAbsent(TextColor.WHITE))
                } else {
                    null
                }
            }
            .ifEmpty { null }
            ?.let { matchComponents -> TextComponent.join(TextComponent.newline(), matchComponents) }
    }
}

// TODO: maybe pull this into a library, it's in ChattORE too (except not RE2J)
private fun <T> String.splitMap(
    pattern: Pattern,
    noMatch: (String) -> T,
    onMatch: (Matcher) -> T,
): Pair<Boolean, MutableList<T>> {
    val m = pattern.matcher(this)
    val result = mutableListOf<T>()
    var i = 0
    var didMatch = false
    do {
        if (!m.find(i)) {
            // nothing matched
            result.add(noMatch(substring(i)))
            break
        }
        // matcher.end() is exclusive
        if (m.end() <= i) {
            // empty match, we can't make progress anymore
            // dunno if this works like it should
            didMatch = true
            result.add(onMatch(m))
            result.add(noMatch(substring(i)))
            break
        }
        val matchStart = m.start()
        if (matchStart != i) {
            // some text before match
            result.add(noMatch(substring(i, matchStart)))
        }
        didMatch = true
        result.add(onMatch(m))
        i = m.end()
    } while (i < length)
    return didMatch to result
}

private fun json2plain(json: String): String = PlainTextComponentSerializer.plainText().serialize(
    GsonComponentSerializer.gson().deserialize(json)
)
