package org.openredstone.redstonetools.org.openredstone.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
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
import kotlin.math.abs

@CommandAlias("/rstack|/rs")
class RStack(private val worldEdit: WorldEditPlugin) : BaseCommand() {
    @Subcommand("-e")
    fun rstackAndExpand(
        player: Player,
        @Default("1") times: Int,
        @Default("2") spacing: Int
    ) {
        doStack(player, times, spacing, true)
    }

    @Default
    fun rstack(
            player: Player,
            @Default("1") times: Int,
            @Default("2") spacing: Int
    ) {
        doStack(player, times, spacing, false)
    }

    // TODO: direction argument?
    private fun doStack(player: Player, times: Int, spacing: Int, expand: Boolean) {
        ensurePositive(times, "stack amount")
        ensurePositive(spacing, "spacing")
        val bukkitPlayer = worldEdit.wrapPlayer(player)
        val session =
                worldEdit.getSession(player) ?: throw ConditionFailedException("Could not get a WorldEdit session")
        val selection = try {
            session.getSelection(session.selectionWorld)
        } catch (e: IncompleteRegionException) {
            throw ConditionFailedException("You do not have a selection!")
        }
        val spacingVec = directionVectorFor(bukkitPlayer).multiply(spacing)
        // dammit?
        val affected = try {
            // worldEdit.remember
            worldEdit.createEditSession(player).use { editSession ->
                val copy = ForwardExtentCopy(editSession, selection, editSession, selection.minimumPoint).apply {
                    repetitions = times
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
            expandSelection(selection, spacingVec.multiply(times), session, bukkitPlayer)
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

    // TODO: dammit?
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
