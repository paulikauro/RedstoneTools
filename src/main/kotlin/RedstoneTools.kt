package redstonetools

import co.aikar.commands.*
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Exception
import java.util.logging.Level

class RedstoneTools : JavaPlugin() {

    private fun handleCommandException(
            command: BaseCommand,
            registeredCommand: RegisteredCommand<*>,
            sender: CommandIssuer,
            args: List<String>,
            throwable: Throwable
    ): Boolean {
        val exception = throwable as? RedstoneToolsException
        if (exception == null) {
            logger.log(Level.SEVERE, "Error in ACF", throwable)
            return false
        }
        val message = exception.message ?: "Something went wrong."
        sender.sendMessage("${ChatColor.DARK_GRAY}[${ChatColor.GRAY}RedstoneTools${ChatColor.DARK_GRAY}]${ChatColor.GRAY} $message")
        return true
    }

    override fun onEnable() {
        val wePlugin = server.pluginManager.getPlugin("WorldEdit")
        if (wePlugin !is WorldEditPlugin) {
            logger.severe("Could not load WorldEdit! RedstoneTools requires WorldEdit to function properly.")
            logger.severe("Disabled.")
            return
        }
        val worldEdit = wePlugin.worldEdit
        server.pluginManager.registerEvents(WorldEditHelper(this, worldEdit), this)
        PaperCommandManager(this).apply {
            commandCompletions.registerCompletion("slabs", SlabCompletionHandler())
            commandCompletions.registerCompletion("find_mask", FindCompletionHandler(worldEdit))
            commandCompletions.registerCompletion("find_page", FindPageCompletionHandler())
            registerThing<SignalStrength>("Signal strength", { SignalStrength.of(it) }, SignalStrength.values)
            registerThing<SignalContainer>("Container", { SignalContainer.of(it) }, SignalContainer.values)
            setDefaultExceptionHandler(::handleCommandException, false)
            registerCommands(
                RStack(worldEdit),
                Find(worldEdit),
                Container(),
                Slab()
            )
        }
    }
}

class RedstoneToolsException(message: String) : Exception(message)

private fun PaperCommandManager.registerCommands(vararg commands: BaseCommand) =
    commands.forEach(::registerCommand)

inline fun <reified T> PaperCommandManager.registerThing(
    readableName: String,
    crossinline create: (String) -> T?,
    values: List<String>
) {
    val name = readableName.replace(" ", "_").toLowerCase()
    val errorMessage = "$readableName must be one of $values"
    commandContexts.registerContext(T::class.java) { context ->
        create(context.popFirstArg()) ?: throw InvalidCommandArgument(errorMessage)
    }
    commandCompletions.registerStaticCompletion(name, values)
    commandCompletions.setDefaultCompletion(name, T::class.java)
}

class SignalStrength(val value: Int) {
    companion object {
        fun of(arg: String): SignalStrength? = when (arg) {
            in hexValues -> SignalStrength(arg.toInt(16))
            in intValues -> SignalStrength(arg.toInt())
            else -> null
        }
        private val intValues = (0..15).map(Int::toString)
        private val hexValues = ('a'..'f').map(Char::toString)
        val values = intValues + hexValues
    }
}

class SignalContainer(val material: Material) {
    companion object {
        // Not a map [yet] cuz we want shortcuts
        // maybe possible to just check first letter (like WorldEdit does with directions)
        // depending on what other containers we want to support
        private val materials = listOf(
            "furnace" to Material.FURNACE,
            "chest" to Material.CHEST,
            "barrel" to Material.BARREL,
            "hopper" to Material.HOPPER
        )
        val values = materials.map { it.first }.sorted()
        fun of(arg: String): SignalContainer? {
            // inefficient but not critical
            for ((name, material) in materials) {
                if (name.startsWith(arg)) {
                    return SignalContainer(material)
                }
            }
            return null
        }
    }
}
