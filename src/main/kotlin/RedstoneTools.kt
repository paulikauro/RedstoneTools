package redstonetools

import co.aikar.commands.PaperCommandManager
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.plugin.java.JavaPlugin

class RedstoneTools : JavaPlugin() {
    override fun onEnable() {
        val wePlugin = server.pluginManager.getPlugin("WorldEdit")
        if (wePlugin == null || wePlugin !is WorldEditPlugin) {
            logger.severe("Could not load WorldEdit! RedstoneTools requires WorldEdit to function properly.")
            logger.severe("Disabled.")
            return
        }
        val rstack = RStack(wePlugin.worldEdit)
        val container = Container()
        PaperCommandManager(this).apply {
            registerCommand(rstack)
            registerCommand(container)
            commandCompletions.registerCompletion("containers", ContainerCompletionHandler())
        }
    }
}