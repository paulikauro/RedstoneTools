package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.annotation.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.Vector

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
        while(!isSafeLocation(newLocation)) {
            newLocation.subtract(direction)
        }
        player.teleport(newLocation)
    }

    private fun isSafeLocation(location: Location): Boolean {
        val feet = location.block
        return !(!feet.type.isAir || !feet.location.add(0.0, 1.0, 0.0).block.type.isAir)
    }
}