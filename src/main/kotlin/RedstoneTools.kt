package org.openredstone.redstonetools

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class RedstoneTools : JavaPlugin(), Listener {
    override fun onEnable() {
        logger.info("it's alive!")
        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        listOf("hi", "how", "are", "you").forEach {
            event.player.sendMessage(it)
        }
    }
}