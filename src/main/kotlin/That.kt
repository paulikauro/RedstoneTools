package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import com.sk89q.worldedit.util.formatting.text.TextComponent

private const val ITERATIONS_LIMIT = 160

@CommandAlias("/that|/hsel")
@Description("Select the build you're looking at")
@CommandPermission("redstonetools.that")
class That(private val worldEdit: WorldEdit) : BaseCommand() {
    @Default
    fun that(
        player: WEPlayer,
        @Default("#existing")
        mask: Mask,
    ) {
        val target = player.getBlockTrace(ITERATIONS_LIMIT)?.toVector()?.toBlockPoint() ?: run {
            player.printError(TextComponent.of("No build in sight!"))
            return
        }

        val (region, iters) = expandRegion(target, mask)
        if (iters == ITERATIONS_LIMIT) {
            player.printError(TextComponent.of("Reached iteration limit while selecting. Your selection may be too small or too big."))
        }
        val sel = CuboidRegionSelector(player.world, region.pos1, region.pos2)
        val session = worldEdit.sessionManager.get(player)
        session.setRegionSelector(player.world, sel)
        sel.explainRegionAdjust(player, session)
        player.printInfo(TextComponent.of("Build selected."))
    }

    private fun expandRegion(target: BlockVector3, mask: Mask): Pair<CuboidRegion, Int> {
        val region = CuboidRegion(target, target)
        // not always the case, but here pos1 = min, pos2 = max
        fun CuboidRegion.xyMin() = CuboidRegion(pos1.withZ(pos1.z - 1), pos2.withZ(pos1.z - 1))
        fun CuboidRegion.xyMax() = CuboidRegion(pos1.withZ(pos2.z + 1), pos2.withZ(pos2.z + 1))
        fun CuboidRegion.xzMin() = CuboidRegion(pos1.withY(pos1.y - 1), pos2.withY(pos1.y - 1))
        fun CuboidRegion.xzMax() = CuboidRegion(pos1.withY(pos2.y + 1), pos2.withY(pos2.y + 1))
        fun CuboidRegion.yzMin() = CuboidRegion(pos1.withX(pos1.x - 1), pos2.withX(pos1.x - 1))
        fun CuboidRegion.yzMax() = CuboidRegion(pos1.withX(pos2.x + 1), pos2.withX(pos2.x + 1))
        fun Region.hasStuff() = any(mask::test)
        val projections = arrayOf(CuboidRegion::xyMin, CuboidRegion::xyMax, CuboidRegion::xzMin, CuboidRegion::xzMax, CuboidRegion::yzMin, CuboidRegion::yzMax)
        val directions = arrayOf(BlockVector3.UNIT_MINUS_Z, BlockVector3.UNIT_Z, BlockVector3.UNIT_MINUS_Y, BlockVector3.UNIT_Y, BlockVector3.UNIT_MINUS_X, BlockVector3.UNIT_X)
        val shouldExpand = projections.map { it(region).hasStuff() }.toBooleanArray()
        fun checkFace(i: Int, doExpand: (BlockVector3) -> Unit, vararg edges: Int) {
            if (!shouldExpand[i]) return
            val proj = projections[i]
            val dir = directions[i]
            for (e in edges) {
                if (proj(projections[e](region)).hasStuff()) {
                    shouldExpand[e] = true
                }
            }
            doExpand(dir)
            shouldExpand[i] = proj(region).hasStuff()
        }
        fun p1(x: BlockVector3) { region.pos1 = region.pos1.add(x) }
        fun p2(x: BlockVector3) { region.pos2 = region.pos2.add(x) }
        var i = 0
        while (shouldExpand.any { it } && i < ITERATIONS_LIMIT) {
            checkFace(0, ::p1, 2, 3, 4, 5)
            checkFace(1, ::p2, 2, 3, 4, 5)
            checkFace(2, ::p1, 0, 1, 4, 5)
            checkFace(3, ::p2, 0, 1, 4, 5)
            checkFace(4, ::p1, 0, 1, 2, 3)
            checkFace(5, ::p2, 0, 1, 2, 3)
            i++
        }
        return region to i
    }
}
