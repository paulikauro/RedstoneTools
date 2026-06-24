package io.github.paulikauro.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.UnknownDirectionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.function.mask.ExistingBlockMask
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.Direction
import kotlin.math.abs

fun PluginScope.createRStack(worldEdit: WorldEdit) {
    commandManager.registerCommand(RStack(worldEdit))
}

private const val DEFAULT_COUNT = 1
private const val DEFAULT_SPACING = 2

@CommandAlias("/rstack|/rs")
@Description("Redstone stacking command")
@CommandPermission("redstonetools.rstack")
private class RStack(private val worldEdit: WorldEdit) : BaseCommand() {
    @Default
    @Syntax("[-ew] [count] ([direction] [spacing] | [spacing vector])")
    fun rstack(
        player: WEPlayer,
        session: LocalSession,
        selection: Region,
        args: Array<String>,
    ) {
        var expand = false
        var withAir = false
        var directionVec: BlockVector3? = null
        var directionStr: String? = null
        var count: Int? = null
        var spacing: Int? = null
        for (arg in args) {
            val int = arg.toIntOrNull()
            when {
                int != null -> when {
                    count == null -> count = int
                    spacing == null -> spacing = int
                    else -> throw InvalidCommandArgument("Too many arguments!")
                }

                arg.all { it.isDigit() || it in "-," } -> {
                    // probably a direction vector
                    if (directionVec != null) throw InvalidCommandArgument("Too many arguments!")
                    directionVec = parseBlockVec(arg)
                }

                arg.startsWith('-') -> {
                    for (flag in arg.drop(1)) {
                        when (flag) {
                            'e' -> expand = true
                            'w' -> withAir = true
                            else -> throw InvalidCommandArgument("Unknown flag: -$flag")
                        }
                    }
                }

                else -> {
                    // probably a direction string
                    if (directionStr != null) throw InvalidCommandArgument("Too many arguments!")
                    directionStr = arg
                }
            }
        }
        if (directionVec == null) {
            directionVec = directionVectorFor(player, directionStr ?: "me").multiply(spacing ?: DEFAULT_SPACING)
        } else if (directionStr != null || spacing != null) {
            throw InvalidCommandArgument("Direction or spacing cannot be used with direction vector")
        }
        count = count ?: DEFAULT_COUNT
        if (count < 0) {
            count *= -1
            directionVec = directionVec.multiply(-1)
        }
        doStack(player, session, selection, count, directionVec, expand, withAir)
    }

    private fun parseBlockVec(arg: String): BlockVector3 {
        val parts = arg.split(',')
        if (parts.size != 3)
            throw InvalidCommandArgument("Direction vector should have 3 coordinates, got ${parts.size}")
        return parts.map { it.toIntOrNull() ?: throw InvalidCommandArgument("Cannot parse coordinate: $it") }
            .let { (x, y, z) -> BlockVector3.at(x, y, z) }
    }

    private fun doStack(
        player: WEPlayer,
        session: LocalSession,
        selection: Region,
        count: Int,
        spacing: BlockVector3,
        expand: Boolean,
        withAir: Boolean,
    ): Int {
        val affected = try {
            session.createEditSession(player).use { editSession ->
                val copy = ForwardExtentCopy(editSession, selection, editSession, selection.minimumPoint).apply {
                    repetitions = count
                    transform = AffineTransform().translate(spacing)
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
        player.info("Operation completed, $affected blocks affected")
        if (expand) {
            expandSelection(selection, spacing.multiply(count), session, player)
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

    private fun directionVectorFor(player: WEPlayer, direction: String): BlockVector3 {
        // TODO: clean this up
        val pitch = when {
            direction == "me" -> player.location.pitch
            // diagonal direction strings, eg. nd (north down) or fu (forward up)
            isDiagDirStr(direction, 'u') -> -25.0f
            isDiagDirStr(direction, 'd') -> 25.0f
            else -> 0.0f
        }
        val vec = try {
            worldEdit.getDiagonalDirection(player, direction)
        } catch (_: UnknownDirectionException) {
            throw InvalidCommandArgument("Unknown direction: $direction")
        }
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

private val BlockVector3.isUpright: Boolean
    get() = x() == 0 && z() == 0
