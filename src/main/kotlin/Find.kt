package io.github.paulikauro.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.function.RegionFunction
import com.sk89q.worldedit.function.RegionMaskingFilter
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.visitor.RegionVisitor
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.text.TextComponent
import java.util.*

fun PluginScope.createFind() {
    val findResults = HashMap<UUID, List<LocationContainer>>()
    commandManager.apply {
        commandCompletions.registerCompletion(COMPLETION_FIND_PAGE, PageCompletionHandler(findResults))
        registerCommand(Find(findResults))
    }
}

private const val COMPLETION_FIND_PAGE = "find_page"

@CommandAlias("/find")
@Description("Find blocks matching a mask in your selection")
@CommandPermission("redstonetools.find")
private class Find(private val findResults: MutableMap<UUID, List<LocationContainer>>) : BaseCommand() {
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
    @CommandCompletion("@$COMPLETION_FIND_PAGE")
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
