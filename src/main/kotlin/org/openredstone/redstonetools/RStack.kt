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
        player.sendMessage("hello ${player.displayName}, expand? $expand, $times many times with spacing $spacing")
        val session =
                worldEdit.getSession(player) ?: throw ConditionFailedException("Could not get a WorldEdit session")
        val selection = try {
            session.getSelection(session.selectionWorld)
        } catch (e: IncompleteRegionException) {
            throw ConditionFailedException("You do not have a selection!")
        }
        val offsetInc = createOffsetIncrement(player, spacing)
        val clipboard = BlockArrayClipboard(selection)
        // dammit?
        try {
            worldEdit.createEditSession(player).use { editSession ->
                // val copy = ForwardExtentCopy(editSession, selection, clipboard, selection.minimumPoint)
                val copy = ForwardExtentCopy(editSession, selection, editSession, selection.minimumPoint)
                //
                copy.repetitions = times
                copy.transform = AffineTransform().translate(offsetInc)
                copy.isCopyingBiomes = false
                copy.isCopyingEntities = false
                copy.isRemovingEntities = false
                copy.sourceMask = ExistingBlockMask(editSession)
                //
                Operations.complete(copy)
                var pos = selection.minimumPoint
                //repeat(times) {
                if (!true) {
                    pos = pos.add(offsetInc)
                    val op = ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(pos)
                            .ignoreAirBlocks(true)
                            .build()
                    Operations.complete(op)
                }
                session.remember(editSession)
            }
        } catch (e: WorldEditException) {
            throw ConditionFailedException("Something went wrong: ${e.message}")
        }

        if (expand) {
            selection.expand(offsetInc.multiply(times))
            val bukkitPlayer = worldEdit.wrapPlayer(player)
            val selector = session.getRegionSelector(bukkitPlayer.world)
            selector.learnChanges()
            selector.explainRegionAdjust(bukkitPlayer, session)
        }
        player.sendMessage("Operation completed")
    }

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
