package org.openredstone.redstonetools.org.openredstone.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitPlayer
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.function.mask.ExistingBlockMask
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.Direction
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.lang.Character.isDigit
import java.lang.Integer.parseInt
import kotlin.math.abs

@CommandAlias("/rstack|/rs")
class RStack(private val worldEdit: WorldEditPlugin) : BaseCommand() {
    @Default
    fun rstack(
            player: Player,
            args: Array<String>
    ) {
        var expand = false
        var direction: String? = null
        val numbers = mutableListOf<Int>()
        for (arg in args) {
            when {
                // don't care about duplicate flags
                arg == "-e" -> expand = true
                arg.all(::isDigit) -> numbers.add(parseInt(arg))
                else -> {
                    // probably a direction string
                    if (direction != null) {
                        throw ConditionFailedException("Too many arguments!")
                    }
                    direction = arg
                }
            }
        }
        if (numbers.size > 2) {
            throw ConditionFailedException("Too many arguments!")
        }
        player.sendMessage(numbers)
        val count = numbers.getOrDefault(0, 1)
        val spacing = numbers.getOrDefault(1, 2)
        ensurePositive(count, "stack amount")
//        ensurePositive(spacing, "spacing")
        doStack(player, count, spacing, expand)
    }

    private fun doStack(player: Player, count: Int, spacing: Int, expand: Boolean) {
        val bukkitPlayer = worldEdit.wrapPlayer(player)
        val session =
                worldEdit.getSession(player) ?: throw ConditionFailedException("Could not get a WorldEdit session")
        val selection = try {
            session.getSelection(session.selectionWorld)
        } catch (e: IncompleteRegionException) {
            throw ConditionFailedException("You do not have a selection!")
        }
        val spacingVec = directionVectorFor(bukkitPlayer).multiply(spacing)
        val affected = try {
            // worldEdit.remember
            worldEdit.createEditSession(player).use { editSession ->
                val copy = ForwardExtentCopy(editSession, selection, editSession, selection.minimumPoint).apply {
                    repetitions = count
                    transform = AffineTransform().translate(spacingVec)
                    isCopyingBiomes = false
                    isCopyingEntities = false
                    isRemovingEntities = false
                    sourceMask = ExistingBlockMask(editSession)
                }
                Operations.complete(copy)
                session.remember(editSession)
                copy.affected
            }
        } catch (e: WorldEditException) {
            throw ConditionFailedException("Something went wrong: ${e.message}")
        }

        if (expand) {
            expandSelection(selection, spacingVec.multiply(count), session, bukkitPlayer)
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "Operation completed, $affected blocks affected")
    }

    private fun expandSelection(selection: Region, amount: BlockVector3, session: LocalSession, player: BukkitPlayer) {
        selection.expand(amount)
        session.getRegionSelector(player.world).apply {
            learnChanges()
            explainRegionAdjust(player, session)
        }
    }

    // TODO: direction argument for command?
    private fun directionVectorFor(player: BukkitPlayer): BlockVector3 {
        // one of N, E, S, W, up, down
        val direction = player.cardinalDirection
        val vec = direction.toBlockVector()
        val pitch = player.location.pitch
        if (direction.isUpright || abs(pitch) <= 22.5) {
            // horizontal or vertical direction
            return vec
        }
        // diagonal direction, need to add the y-component
        // negative pitch is downwards
        return vec.add(
                if (pitch < 0) {
                    Direction.UP
                } else {
                    Direction.DOWN
                }.toBlockVector()
        )
    }

    private fun ensurePositive(arg: Int, name: String) {
        if (arg > 0) {
            return
        }
        throw ConditionFailedException("$name must be positive! $arg is not")
    }
}

private fun <E> List<E>.getOrDefault(index: Int, default: E): E = getOrNull(index) ?: default

