package org.openredstone.redstonetools.org.openredstone.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.entity.Player

@CommandAlias("/rstack|/rs")
class RStack(private val worldEdit: WorldEditPlugin) : BaseCommand() {
    @Default
    @CommandCompletion("[-e]")
    fun rstack(
            player: Player,
            @Optional expand: String?,
            @Default("1") times: Int,
            @Default("2") spacing: Int
    ) {
        val ex = expand ?: "no expand"
        player.sendMessage("hello $ex, $times many times with spacing $spacing")
    }
}
