package io.github.paulikauro.redstonetools

import co.aikar.commands.*
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.component.PaginationBox
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import de.tr7zw.nbtapi.NBT
import de.tr7zw.nbtapi.iface.ReadWriteNBT
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.ComponentBuilderApplicable
import net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE
import net.kyori.adventure.text.format.NamedTextColor.RED
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.math.ceil

operator fun String.get(s: ComponentBuilderApplicable): Component =
    text().content(this).applicableApply(s).build()

operator fun ComponentBuilderApplicable.times(other: ComponentBuilderApplicable) = ComponentBuilderApplicable {
    it.applicableApply(this)
    it.applicableApply(other)
}

// overloads for WEPlayer until WE migrates to Adventure
fun Audience.info(msg: String): Unit = sendMessage(msg[LIGHT_PURPLE])
fun WEPlayer.info(msg: String): Unit = sendMessage(msg[LIGHT_PURPLE])
fun Audience.err(msg: String): Unit = sendMessage(msg[RED])
fun WEPlayer.err(msg: String): Unit = sendMessage(msg[RED])

typealias WEPlayer = com.sk89q.worldedit.entity.Player

fun Player.we(): WEPlayer = BukkitAdapter.adapt(this)
fun WEPlayer.bukkit(): Player = BukkitAdapter.adapt(this)

fun WEPlayer.sendMessage(message: Component): Unit = bukkit().sendMessage(message)

fun Location.toBlockVector3(): BlockVector3 = BlockVector3.at(x, y, z)

// 😎
@Suppress("UNCHECKED_CAST")
inline fun <T : ItemMeta> ItemStack.modifyMeta(action: T.() -> Unit) {
    itemMeta = (itemMeta as T).apply(action)
}

fun ItemStack.modifyComponents(action: ReadWriteNBT.() -> Unit) = this.also { NBT.modifyComponents(this, action) }

fun parseMaskOrThrow(arg: String, worldEdit: WorldEdit, localSession: LocalSession?, player: WEPlayer?): Mask {
    val parserContext = ParserContext().apply {
        actor = player
        world = player?.world
        session = localSession
        extent = player?.world
        isRestricted = true
    }
    return worldEdit.maskFactory.parseFromInput(arg, parserContext)
}

fun LocalSession.getSelectionOrNull(): Region? = try {
    selection
} catch (_: IncompleteRegionException) {
    null
}

const val MAKE_SELECTION_FIRST = "Make a region selection first."

fun LocalSession.requireSelection(): Region =
    getSelectionOrNull() ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)

const val COMPLETION_MASK = "we_mask"

fun PluginScope.registerWECommandContexts(worldEdit: WorldEdit) = commandManager.apply {
    commandCompletions.registerCompletion(COMPLETION_MASK, MaskCompletionHandler(worldEdit))
    commandCompletions.setDefaultCompletion(COMPLETION_MASK, Mask::class.java)
    commandContexts.registerContext(Mask::class.java) { context ->
        val player = context.player?.we()
        val localSession = player?.let(worldEdit.sessionManager::get)
        parseMaskOrThrow(context.popFirstArg(), worldEdit, localSession, player)
    }

    fun BukkitCommandExecutionContext.requireWEPlayer(): WEPlayer =
        player?.we() ?: throw ConditionFailedException("This can only be run by a player")

    fun BukkitCommandExecutionContext.requireWESession(): LocalSession =
        worldEdit.sessionManager.get(requireWEPlayer())

    commandContexts.registerIssuerOnlyContext(WEPlayer::class.java) { context ->
        context.requireWEPlayer()
    }
    commandContexts.registerIssuerOnlyContext(LocalSession::class.java) { context ->
        context.requireWESession()
    }
    commandContexts.registerIssuerOnlyContext(Region::class.java) { context ->
        context.requireWESession().requireSelection()
    }
}

private class MaskCompletionHandler(private val worldEdit: WorldEdit) :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> =
        worldEdit.maskFactory.getSuggestions(
            context.input,
            ParserContext().apply { actor = BukkitAdapter.adapt(context.player) }
        )
}

data class LocationContainer(val location: BlockVector3, val match: TextComponent)

class LocationsPaginationBox(private val locations: List<LocationContainer>, title: String, command: String) :
    PaginationBox(title, command) {

    init {
        setComponentsPerPage(7)
    }

    override fun getComponent(number: Int): com.sk89q.worldedit.util.formatting.text.Component {
        if (number > locations.size) throw IllegalArgumentException("Invalid location index.")
        return TextComponent.of("${number + 1}: ")
            .append(locations[number].match)
            .color(TextColor.LIGHT_PURPLE)
            .clickEvent(locations[number].location.run { ClickEvent.runCommand("/tp ${x()} ${y()} ${z()}") })
            .hoverEvent(HoverEvent.showText(TextComponent.of("Click to teleport")))
    }

    override fun getComponentsSize(): Int = locations.size

    override fun create(page: Int): com.sk89q.worldedit.util.formatting.text.Component {
        super.getContents()
            .append(TextComponent.of("Total Results: ${locations.size}").color(TextColor.GRAY))
            .append(TextComponent.newline())
        return super.create(page)
    }
}

class PageCompletionHandler(private val results: Map<UUID, List<LocationContainer>>) :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
        val player = context.player ?: return emptyList()
        val locations = results[player.uniqueId] ?: return emptyList()
        return (1..ceil(locations.size / 7f).toInt()).map { it.toString() }
    }
}

class PluginScope(val plugin: Plugin, val commandManager: PaperCommandManager) {
    val pluginManager get() = plugin.server.pluginManager
}

class RedstoneToolsException(override val message: String) : Exception(message)

interface Thing<T> {
    val readableName: String
    fun of(arg: String): T?
    val values: Collection<String>
    val valueClass: Class<T>
}

fun <T> PaperCommandManager.registerThing(thing: Thing<T>) {
    val name = thing.readableName.replace(" ", "_").lowercase()
    val errorMessage = "${thing.readableName} must be one of ${thing.values}"
    commandContexts.registerContext(thing.valueClass) { context ->
        thing.of(context.popFirstArg()) ?: throw InvalidCommandArgument(errorMessage)
    }
    commandCompletions.apply {
        registerStaticCompletion(name, thing.values)
        setDefaultCompletion(name, thing.valueClass)
    }
}
