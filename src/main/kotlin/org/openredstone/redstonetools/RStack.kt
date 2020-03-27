package org.openredstone.redstonetools.org.openredstone.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.function.mask.ExistingBlockMask
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.session.ClipboardHolder
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
        ensurePositive(times, "stack amount")
        ensurePositive(spacing, "spacing")
        // TODO: factor things out
        val session =
                worldEdit.getSession(player) ?: throw ConditionFailedException("Could not get a WorldEdit session")
        val selection = try {
            session.getSelection(session.selectionWorld)
        } catch (e: IncompleteRegionException) {
            throw ConditionFailedException("You do not have a selection!")
        }
        val offsetInc = createOffsetIncrement(player, spacing)
        // dammit?
        val affected = try {
            worldEdit.createEditSession(player).use { editSession ->
                val copy = ForwardExtentCopy(editSession, selection, editSession, selection.minimumPoint).apply {
                    repetitions = times
                    transform = AffineTransform().translate(offsetInc)
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
            // TODO: maybe not a good exception?
            throw ConditionFailedException("Something went wrong: ${e.message}")
        }

        if (expand) {
            // TODO: factor this out
            selection.expand(offsetInc.multiply(times))
            val bukkitPlayer = worldEdit.wrapPlayer(player)
            val selector = session.getRegionSelector(bukkitPlayer.world)
            selector.learnChanges()
            selector.explainRegionAdjust(bukkitPlayer, session)
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "Operation completed, $affected blocks affected")
    }

    // TODO: dammit?
    // TODO: createOffsetIncrement -> getDirection (possibly argument, like north, me, west, ...)
    // TODO: dedupe bukkitPlayer
    private fun createOffsetIncrement(player: Player, spacing: Int): BlockVector3 {
        val bukkitPlayer = worldEdit.wrapPlayer(player)
        val direction = bukkitPlayer.cardinalDirection

        assert(!direction.isSecondaryOrdinal)

        var offsetIncrement = direction.toBlockVector()

        val pitch = player.location.pitch
        if (abs(pitch) > 22.5 && !direction.isUpright) {
            // diagonal pitch, so add the y-component
            // negative pitch is downwards
            offsetIncrement = offsetIncrement.add(if (pitch < 0) {
                Direction.UP
            } else {
                Direction.DOWN
            }.toBlockVector())
        }
        return offsetIncrement.multiply(spacing)
    }

    private fun ensurePositive(arg: Int, name: String) {
        if (arg > 0) {
            return
        }
        throw ConditionFailedException("$name must be positive! $arg is not")
    }
}
