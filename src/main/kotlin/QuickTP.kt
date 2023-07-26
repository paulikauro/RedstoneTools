package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.annotation.*
import org.bukkit.Material
import org.bukkit.entity.Player

@CommandAlias("qtp|quicktp")
@Description("Quick Teleport Command")
@CommandPermission("redstonetools.quicktp")
class QuickTP : BaseCommand() {
    @Default
    @Syntax("[distance]")
    fun quicktp(player: Player, @Default("32") distance: Int)
    {
        if (distance < 1 || distance > 64){
            throw InvalidCommandArgument("Distance must be between 1 and 64")
        }
        val direction = player.location.direction
        val newLocation = player.location.add(direction.multiply(distance))
        player.teleport(newLocation)
    }
}