package redstonetools

import co.aikar.commands.*
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.extension.factory.MaskFactory
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.component.PaginationBox
import com.sk89q.worldedit.util.formatting.text.Component
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class RedstoneTools : JavaPlugin() {
    private fun handleCommandException(
        command: BaseCommand,
        registeredCommand: RegisteredCommand<*>,
        sender: CommandIssuer,
        args: List<String>,
        throwable: Throwable
    ): Boolean = when (throwable) {
        is RedstoneToolsException, is WorldEditException -> {
            val message = throwable.message ?: "Something went wrong."
            sender.sendMessage("${ChatColor.DARK_GRAY}[${ChatColor.GRAY}RedstoneTools${ChatColor.DARK_GRAY}]${ChatColor.GRAY} $message")
            true
        }
        else -> {
            logger.log(Level.SEVERE, "handleCommandException", throwable)
            false
        }
    }

    override fun onEnable() {
        saveDefaultConfig()
        val thatSection = config.getConfigurationSection("that") ?: config.createSection("that")
        val thatConfig = thatSection.run {
            // note the defaults are duplicated in here, config.yml and ThatConfig, it's a temporary solution
            ThatConfig(
                sizeLimit = getInt("sizeLimit", 200),
                maxTimePerTickMs = getInt("maxTimePerTickMs", 30),
                maxTicks = getInt("maxTicks", 5),
            )
        }
        val wePlugin = server.pluginManager.getPlugin("WorldEdit")
        if (wePlugin !is WorldEditPlugin) {
            logger.severe("Could not load WorldEdit! RedstoneTools requires WorldEdit to function properly.")
            // TODO: actually disable?
            logger.severe("Disabled.")
            return
        }
        val worldEdit = wePlugin.worldEdit
        val liveStack = LiveStack(this, worldEdit)
        val autowire = Autowire(server.pluginManager, liveStack, this)
        val pins = PinCommand(this)
        val autoRotate = AutoRotate()
        val cauldron = Cauldron()
        arrayOf(
            WorldEditHelper(this, worldEdit),
            SlabListener(),
            autowire,
            autoRotate,
            cauldron,
            liveStack,
            // noo
            pins.listener,
        ).forEach { server.pluginManager.registerEvents(it, this) }
        PaperCommandManager(this).apply {
            arrayOf(
                "slabs" to SlabCompletionHandler(),
                "we_mask" to MaskCompletionHandler(worldEdit),
                "find_page" to FindPageCompletionHandler(),
                "search_page" to SearchPageCompletionHandler(),
                "pins" to pins.CompletionHandler(),
            ).forEach { (id, handler) -> commandCompletions.registerCompletion(id, handler) }
            commandCompletions.setDefaultCompletion("we_mask", Mask::class.java)
            commandContexts.registerContext(Mask::class.java) { context ->
                val player = context.player?.let(BukkitAdapter::adapt)
                val localSession = player?.let(worldEdit.sessionManager::get)
                parseMaskOrThrow(context.popFirstArg(), worldEdit, localSession, player)
            }
            fun BukkitCommandExecutionContext.requireWEPlayer(): com.sk89q.worldedit.entity.Player =
                player?.let(BukkitAdapter::adapt) ?: throw ConditionFailedException("This can only be run by a player")
            commandContexts.registerIssuerOnlyContext(com.sk89q.worldedit.entity.Player::class.java) { context ->
                context.requireWEPlayer()
            }
            commandContexts.registerIssuerOnlyContext(LocalSession::class.java) { context ->
                worldEdit.sessionManager.get(context.requireWEPlayer())
            }
            commandContexts.registerIssuerOnlyContext(Region::class.java) { context ->
                worldEdit.sessionManager.get(context.requireWEPlayer()).requireSelection()
            }
            arrayOf(
                SignalStrength,
                PinState,
                SignalContainer,
            ).forEach { registerThing(it) }
            setDefaultExceptionHandler(::handleCommandException, false)
            arrayOf(
                RStack(worldEdit),
                Find(worldEdit),
                That(thatConfig, worldEdit, this@RedstoneTools),
                SignSearch(worldEdit),
                Container(),
                Slab(),
                autowire,
                autoRotate,
                cauldron,
                liveStack,
                pins,
                SelectionStack(worldEdit),
            ).forEach(::registerCommand)
        }
    }
}

class RedstoneToolsException(message: String) : Exception(message)

private interface Thing<T> {
    val readableName: String
    fun of(arg: String): T?
    val values: Collection<String>
    val valueClass: Class<T>
}

private fun <T> PaperCommandManager.registerThing(thing: Thing<T>) {
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

class SignalStrength(val value: Int, val originalName: String) {
    companion object : Thing<SignalStrength> {
        override fun of(arg: String): SignalStrength? = when (arg.lowercase()) {
            in hexValues -> SignalStrength(arg.toInt(16), arg)
            in intValues -> SignalStrength(arg.toInt(), arg)
            else -> null
        }

        private val intValues = (0..15).map(Int::toString)
        private val hexValues = ('a'..'f').map(Char::toString)
        override val values = intValues + hexValues
        override val readableName = "Signal strength"
        override val valueClass = SignalStrength::class.java
    }
}

class PinState(val value: Boolean) {
    override fun toString(): String = when (value) {
        false -> "off"
        true -> "on"
    }

    fun not(): PinState = PinState(!value)

    companion object : Thing<PinState> {
        override val readableName = "Pin state"

        override fun of(arg: String): PinState? = when (arg) {
            "on" -> PinState(true)
            "off" -> PinState(false)
            else -> null
        }

        override val values = listOf("on", "off")
        override val valueClass = PinState::class.java
    }
}

class SignalContainer(val material: Material) {
    companion object : Thing<SignalContainer> {
        // Not a map [yet] cuz we want shortcuts
        // maybe possible to just check first letter (like WorldEdit does with directions)
        // depending on what other containers we want to support
        private val materials = listOf(
            "furnace" to Material.FURNACE,
            "chest" to Material.CHEST,
            "barrel" to Material.BARREL,
            "hopper" to Material.HOPPER,
            "jukebox" to Material.JUKEBOX,
        )
        override val values = materials.map { it.first }.sorted()
        override fun of(arg: String): SignalContainer? = materials
            .firstOrNull { (name, _) -> name.startsWith(arg) }
            ?.let { (_, material) -> SignalContainer(material) }

        override val readableName = "Container"
        override val valueClass = SignalContainer::class.java
    }
}

class MaskCompletionHandler(worldEdit: WorldEdit) :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    private val maskFactory = MaskFactory(worldEdit)
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> =
        maskFactory.getSuggestions(context.input)
}

data class LocationContainer(val location: BlockVector3, val match: TextComponent)

class LocationsPaginationBox(private val locations: MutableList<LocationContainer>, title: String, command: String) :
    PaginationBox("${ChatColor.LIGHT_PURPLE}$title", command) {

    init {
        setComponentsPerPage(7)
    }

    override fun getComponent(number: Int): Component {
        if (number > locations.size) throw IllegalArgumentException("Invalid location index.")
        return TextComponent.of("${number + 1}: ")
            .append(locations[number].match)
            .color(TextColor.LIGHT_PURPLE)
            .clickEvent(locations[number].location.run { ClickEvent.runCommand("/tp $x $y $z") })
            .hoverEvent(HoverEvent.showText(TextComponent.of("Click to teleport")))
    }

    override fun getComponentsSize(): Int = locations.size

    override fun create(page: Int): Component {
        super.getContents()
            .append(TextComponent.of("Total Results: ${locations.size}").color(TextColor.GRAY))
            .append(TextComponent.newline())
        return super.create(page)
    }
}
