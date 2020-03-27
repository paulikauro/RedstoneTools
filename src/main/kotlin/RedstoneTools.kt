package org.openredstone.redstonetools

import co.aikar.commands.PaperCommandManager
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.plugin.java.JavaPlugin
import org.openredstone.redstonetools.org.openredstone.redstonetools.RStack

class RedstoneTools : JavaPlugin() {
    override fun onEnable() {
        val maybeWE = server.pluginManager.getPlugin("WorldEdit")
        if (maybeWE == null) {
            logger.severe("Could not find WorldEdit! RedstoneTools requires WorldEdit to function properly.")
            // TODO
            return
        }
        val worldEdit = maybeWE as WorldEditPlugin
        val commandManager = PaperCommandManager(this)
        val rstack = RStack(worldEdit)
        commandManager.registerCommand(rstack)
    }
}