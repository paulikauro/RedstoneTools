package redstonetools

import co.aikar.commands.*
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class RedstoneTools : JavaPlugin() {
    override fun onEnable() {
        val wePlugin = server.pluginManager.getPlugin("WorldEdit")
        if (wePlugin !is WorldEditPlugin) {
            logger.severe("Could not load WorldEdit! RedstoneTools requires WorldEdit to function properly.")
            logger.severe("Disabled.")
            return
        }
        PaperCommandManager(this).apply {
            registerCommands(
                RStack(wePlugin.worldEdit),
                Container(),
                Slab()
            )
            commandCompletions.registerCompletion("slabs", SlabCompletionHandler())
            registerThing("Signal strength", SignalStrength::of, SignalStrength.values)
            registerThing("Container", SignalContainer::of, SignalContainer.values)
        }
    }
}

private fun PaperCommandManager.registerCommands(vararg commands: BaseCommand) =
    commands.forEach(::registerCommand)

inline fun <reified T> PaperCommandManager.registerThing(
    readableName: String,
    crossinline create: (String) -> T?,
    values: List<String>
) {
    val name = readableName.replace(" ", "").toLowerCase()
    val errorMessage = "$readableName must be one of $values"
    commandContexts.registerContext(T::class.java) { context ->
        create(context.popFirstArg()) ?: throw InvalidCommandArgument(errorMessage)
    }
    commandCompletions.registerStaticCompletion(name, values)
    commandCompletions.setDefaultCompletion(name, T::class.java)
}

inline class SignalStrength(val value: Int) {
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

inline class SignalContainer(val material: Material) {
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
