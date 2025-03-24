package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.*
import com.sk89q.worldedit.function.mask.ExistingBlockMask
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.Direction
import com.sk89q.worldedit.util.formatting.text.TextComponent
import java.lang.Integer.parseInt
import kotlin.math.abs

typealias WEPlayer = com.sk89q.worldedit.entity.Player

private val BlockVector3.isUpright: Boolean
    get() = x == 0 && z == 0

@CommandAlias("/rstack|/rs")
@Description("Redstone stacking command")
@CommandPermission("redstonetools.rstack")
class RStack(private val worldEdit: WorldEdit) : BaseCommand() {
    @Default
    @Syntax("[-e] [direction] [count] [spacing]")
    fun rstack(
        player: WEPlayer,
        session: LocalSession,
        selection: Region,
        args: Array<String>
    ) {
        var expand = false
        var withAir = false
        var direction: String? = null
        val numbers = mutableListOf<Int>()
        for (arg in args) {
            when {
                // don't care about duplicate flags
                arg == "-e" -> expand = true
                arg == "-w" -> withAir = true
                arg.all { it.isDigit() || it in "+-" } -> numbers.add(parseInt(arg))
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
        var count = numbers.getOrNull(0) ?: 1
        var spacing = numbers.getOrNull(1) ?: 2
        if (count < 0) {
            count *= -1
            spacing *= -1
        }
        try {
            doStack(player, session, selection, count, spacing, expand, withAir, direction ?: "me")
        } catch (e: UnknownDirectionException) {
            throw ConditionFailedException("Unknown direction")
        }
    }

    // throws UnknownDirectionException
    private fun doStack(
        player: WEPlayer,
        session: LocalSession,
        selection: Region,
        count: Int,
        spacing: Int,
        expand: Boolean,
        withAir: Boolean,
        direction: String
    ): Int {
        val spacingVec = directionVectorFor(player, direction).multiply(spacing)
        val affected = try {
            session.createEditSession(player).use { editSession ->
                val copy = ForwardExtentCopy(editSession, selection, editSession, selection.minimumPoint).apply {
                    repetitions = count
                    transform = AffineTransform().translate(spacingVec)
                    isCopyingBiomes = false
                    isCopyingEntities = false
                    isRemovingEntities = false
                    if (!withAir) {
                        sourceMask = ExistingBlockMask(editSession)
                    }
                }
                Operations.complete(copy)
                session.remember(editSession)
                // TODO: flush block bag?
                copy.affected
            }
        } catch (e: WorldEditException) {
            throw ConditionFailedException("Something went wrong: ${e.message}")
        }
        player.printInfo(TextComponent.of("Operation completed, $affected blocks affected"))
        if (expand) {
            expandSelection(selection, spacingVec.multiply(count), session, player)
        }
        return affected
    }

    private fun expandSelection(selection: Region, amount: BlockVector3, session: LocalSession, player: WEPlayer) {
        selection.expand(amount)
        session.getRegionSelector(player.world).apply {
            learnChanges()
            explainRegionAdjust(player, session)
        }
    }

    // throws UnknownDirectionException
    private fun directionVectorFor(player: WEPlayer, direction: String): BlockVector3 {
        // TODO: clean this up
        val pitch = when {
            direction == "me" -> player.location.pitch
            // diagonal direction strings, eg. nd (north down) or fu (forward up)
            isDiagDirStr(direction, 'u') -> -25.0f
            isDiagDirStr(direction, 'd') -> 25.0f
            else -> 0.0f
        }
        val vec = worldEdit.getDiagonalDirection(player, direction)
        if (vec.isUpright || abs(pitch) <= 22.5) {
            // horizontal or vertical direction
            return vec
        }
        // diagonal direction, need to add the y-component
        // negative pitch is upwards
        return vec.add(
            if (pitch < 0) {
                Direction.UP
            } else {
                Direction.DOWN
            }.toBlockVector()
        )
    }

    private fun isDiagDirStr(direction: String, upOrDown: Char) =
        // check length, because 'd' and 'u' alone are not diagonal directions (they're just up or down)
        direction.length > 1 && direction.last().lowercaseChar() == upOrDown
}
