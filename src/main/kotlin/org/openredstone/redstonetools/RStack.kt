package org.openredstone.redstonetools.org.openredstone.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.entity.Player

@CommandAlias("/rstack|/rs")
class RStack(private val worldEdit: WorldEditPlugin) : BaseCommand() {
    @Default
    fun rstack(
            player: Player,
            @Default("1") times: Int,
            @Default("false") expand: Boolean,
            @Default("2") spacing: Int
    ): Boolean {
        return false
    }
}
