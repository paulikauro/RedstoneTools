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
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.component.PaginationBox
import com.sk89q.worldedit.util.formatting.text.Component
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.lang.IllegalArgumentException
import java.util.UUID
import kotlin.collections.HashMap
import kotlin.math.ceil

val findResults = HashMap<UUID, MutableList<BlockVector3>>()
const val MAKE_SELECTION_FIRST = "Make a region selection first."

@CommandAlias("/find")
@Description("Find some shid in selecton")
@CommandPermission("redstonetools.find")
class Find(private val worldEdit: WorldEdit) : BaseCommand() {

    @Default
    @CommandCompletion("@find_mask")
    @Syntax("[material]")
    fun find(
        player: Player,
        arg: String
    ) {
        doFind(BukkitAdapter.adapt(player), arg)
    }

    @Subcommand("page")
    @CommandCompletion("@find_page")
    @Syntax("[number]")
    fun page(
        player: Player,
        page: Int
    ) {
        val locations = findResults[player.uniqueId] ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
        val paginationBox = FindPaginationBox(locations, "Results", "//find page %page%")
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
        val locations = mutableListOf<BlockVector3>()
        val maskFactory = MaskFactory(worldEdit)
        val parserContext = ParserContext().apply {
            extent = session.selectionWorld
        }
        val blockMask = maskFactory.parseFromInput(arg, parserContext)
        val regionFunction = RegionFunction { position ->
            locations.add(position)
            false
        }
        val regionMaskingFilter = RegionMaskingFilter(blockMask, regionFunction)
        val regionVisitor = RegionVisitor(selection, regionMaskingFilter)
        Operations.complete(regionVisitor)
        findResults[player.uniqueId] = locations
        page(BukkitAdapter.adapt(player), 1)
    }
}

class FindPaginationBox(private val locations: MutableList<BlockVector3>, title: String, command: String):
        PaginationBox("${ChatColor.LIGHT_PURPLE}$title", command)
{
    override fun getComponent(number: Int): Component {
        if (number > locations.size) throw IllegalArgumentException("Invalid location index.")
        return TextComponent.of("${number}: ${locations[number]}")
            .color(TextColor.LIGHT_PURPLE)
            .clickEvent(ClickEvent.runCommand("/tp ${locations[number].x} ${locations[number].y} ${locations[number].z}"))
            .hoverEvent(HoverEvent.showText(TextComponent.of("Click to teleport")))
    }
    override fun getComponentsSize(): Int = locations.size
}

class FindPageCompletionHandler:
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>
{
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
        val player = context.sender as Player
        val locations = findResults[player.uniqueId] ?: return emptyList()
        return (1..ceil(locations.size/8f).toInt()).map { it.toString() }.toList()
    }
}

class FindCompletionHandler(worldEdit: WorldEdit):
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>
{
    private val maskFactory = MaskFactory(worldEdit)
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
        return maskFactory.getSuggestions(context.input)
    }
}