package org.openredstone.redstonetools.org.openredstone.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.entity.Player

@CommandAlias("/rstack|/rs")
class RStack(private val worldEdit: WorldEditPlugin) : BaseCommand() {
    @Subcommand("-e")
    fun rstackAndExpand(
        player: Player,
        @Default("1") times: Int,
        @Default("2") spacing: Int
    ) {
        doit(player, times, spacing, true)
    }

    @Default
    fun rstack(
            player: Player,
            @Default("1") times: Int,
            @Default("2") spacing: Int
    ) {
        doit(player, times, spacing, false)
    }

    fun doit(player: Player, times: Int, spacing: Int, expand: Boolean) {
        player.sendMessage("hello ${player.displayName}, expand? $expand, $times many times with spacing $spacing")
    }
}
