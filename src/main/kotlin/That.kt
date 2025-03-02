package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.world.World
import org.bukkit.entity.Player

private const val ITERATIONS_LIMIT = 160

@CommandAlias("/that|/hsel")
@Description("Select the build you're looking at")
@CommandPermission("redstonetools.that")
class That(private val worldEdit: WorldEdit) : BaseCommand() {
    @Default
    @Syntax("")
    fun that(
        player: Player,
        args: Array<String>
    ) {
        if (args.isNotEmpty()) {
            throw ConditionFailedException("Too many arguments!")
        }

        val weplayer = BukkitAdapter.adapt(player)

        if (selectTargetBlock(weplayer)) {
            growSelection(weplayer)
            weplayer.printInfo(TextComponent.of("Build selected."))
        } else weplayer.printError(TextComponent.of("No build in sight!"))
    }

    private fun selectTargetBlock(player: WEPlayer): Boolean {
        val session = worldEdit.sessionManager.get(player)
        val world = player.world
        val regionSelector = session.getRegionSelector(world)
        val targetBlock = player.getBlockTrace(ITERATIONS_LIMIT)?.toVector()?.toBlockPoint() ?: return false

        regionSelector.selectPrimary(targetBlock, null)
        regionSelector.selectSecondary(targetBlock, null)
        regionSelector.explainRegionAdjust(player, session)
        return true
    }

    private fun growSelection(player: WEPlayer) {
        val session = worldEdit.sessionManager.get(player)
        val world = player.world
        val regionSelector = session.getRegionSelector(world)
        val selection = session.getSelection(world)

        fun checkFace(
            bounds: (Region) -> Pair<BlockVector2, BlockVector2>,
            expandDirection: BlockVector3,
            edge: Region.(Int, Int) -> BlockVector3,
        ): Boolean {
            val (min, max) = bounds(selection)
            for (x in min.x..max.x) {
                for (y in min.z..max.z) {
                    if (mask.test(selection.edge(x, y))) {
                        selection.expand(expandDirection)
                        return false
                    }
                }
            }
            return true
        }
        var emptyFaces = 0
        var iterations = 0

        fun Region.zy() = BlockVector2.at(minimumPoint.z, minimumPoint.y) to BlockVector2.at(maximumPoint.z, maximumPoint.y)
        fun Region.xz() = BlockVector2.at(minimumPoint.x, minimumPoint.z) to BlockVector2.at(maximumPoint.x, maximumPoint.z)
        fun Region.xy() = BlockVector2.at(minimumPoint.x, minimumPoint.y) to BlockVector2.at(maximumPoint.x, maximumPoint.y)

        while (emptyFaces != 6 && iterations < ITERATIONS_LIMIT) {
            iterations++
            emptyFaces = 0

            if (checkFace(Region::zy, BlockVector3.UNIT_X) { x, y -> BlockVector3.at(maximumPoint.x, y, x) }) emptyFaces++
            if (checkFace(Region::zy, BlockVector3.UNIT_MINUS_X) { x, y -> BlockVector3.at(minimumPoint.x, y, x) }) emptyFaces++
            if (checkFace(Region::xz, BlockVector3.UNIT_Y) { x, y -> BlockVector3.at(x, maximumPoint.y, y) }) emptyFaces++
            if (checkFace(Region::xz, BlockVector3.UNIT_MINUS_Y) { x, y -> BlockVector3.at(x, minimumPoint.y, y) }) emptyFaces++
            if (checkFace(Region::xy, BlockVector3.UNIT_Z) { x, y -> BlockVector3.at(x, y, maximumPoint.z) }) emptyFaces++
            if (checkFace(Region::xy, BlockVector3.UNIT_MINUS_Z) { x, y -> BlockVector3.at(x, y, minimumPoint.z) }) emptyFaces++
        }

        regionSelector.learnChanges()
        regionSelector.explainRegionAdjust(player, session)
    }

}
