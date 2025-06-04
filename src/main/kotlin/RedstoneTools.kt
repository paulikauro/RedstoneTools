package redstonetools

import co.aikar.commands.*
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import net.kyori.adventure.extra.kotlin.plus
import net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class RedstoneTools : JavaPlugin() {
    private fun handleCommandException(
        command: BaseCommand,
        registeredCommand: RegisteredCommand<*>,
        sender: CommandIssuer,
        args: List<String>,
        throwable: Throwable,
    ): Boolean = when (throwable) {
        is RedstoneToolsException, is WorldEditException -> {
            val message = throwable.message ?: "Something went wrong."
            sender.getIssuer<CommandSender>()
                .sendMessage("["[DARK_GRAY] + "RedstoneTools"[GRAY] + "] "[DARK_GRAY] + message[GRAY])
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
        PluginScope(this, PaperCommandManager(this)).apply {
            commandManager.apply {
                enableUnstableAPI("help")
                setDefaultExceptionHandler(::handleCommandException, false)
            }
            registerWECommandContexts(worldEdit)
            createAutowire()
            createPins()
            createAutoRotate()
            createCauldron()
            createSlab()
            createWEHelper(worldEdit)
            createFind()
            createSignSearch()
            createRStack(worldEdit)
            createThat(thatConfig, worldEdit)
            createContainer()
            createSelectionStack()
        }
    }
}
