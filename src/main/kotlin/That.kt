package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import com.sk89q.worldedit.util.collection.BlockMap
import com.sk89q.worldedit.util.formatting.text.TextComponent
import kotlin.system.measureTimeMillis
import kotlin.time.measureTimedValue

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
        // NOTE: this does not use the mask!
        val target = player.getBlockTrace(ITERATIONS_LIMIT)?.toVector()?.toBlockPoint() ?: run {
            player.printError(TextComponent.of("No build in sight!"))
            return
        }

        measureTimeMillis {
            expandRegionOld(target, mask)
        }.let { player.printInfo(TextComponent.of("old: $it ms")) }
        measureTimeMillis {
            expandRegionManual(target, mask)
        }.let { player.printInfo(TextComponent.of("manual: $it ms")) }
        val (region, iters) = measureTimedValue {  expandRegionNew(target, mask) }.let {
            player.printInfo(TextComponent.of("new: ${it.duration.inWholeMilliseconds} ms"))
            it.value
        }
        if (iters == ITERATIONS_LIMIT) {
            player.printError(TextComponent.of("Reached iteration limit while selecting. Your selection may be too small or too big."))
        }
        val sel = CuboidRegionSelector(player.world, region.pos1, region.pos2)
        val session = worldEdit.sessionManager.get(player)
        session.setRegionSelector(player.world, sel)
        sel.explainRegionAdjust(player, session)
        player.printInfo(TextComponent.of("Build selected."))
    }

    private fun expandRegionManual(target: BlockVector3, mask: Mask): Pair<CuboidRegion, Int> {
        var min = target
        var max = target
        val visited = BlockMap.create<Unit>()
        val queue = ArrayDeque<BlockVector3>()
        queue.add(target)
        visited[target] = Unit
        val offsets = arrayOf(
            BlockVector3.UNIT_X,
            BlockVector3.UNIT_Y,
            BlockVector3.UNIT_Z,
            BlockVector3.UNIT_MINUS_X,
            BlockVector3.UNIT_MINUS_Y,
            BlockVector3.UNIT_MINUS_Z,
        )
        while (queue.isNotEmpty() && visited.size < ITERATIONS_LIMIT * ITERATIONS_LIMIT * ITERATIONS_LIMIT) {
            val pos = queue.removeFirst()
            min = min.getMinimum(pos)
            max = max.getMaximum(pos)
            for (it in offsets) {
                val newPos = pos.add(it)
                if (newPos in visited) continue
                visited[newPos] = Unit
                if (mask.test(newPos)) {
                    queue.addLast(newPos)
                }
            }
        }
        return CuboidRegion(min, max) to 0
    }

    companion object {
        private const val PX = 0
        private const val MX = 1
        private const val PY = 2
        private const val MY = 3
        private const val PZ = 4
        private const val MZ = 5
    }
    private fun expandRegionNewBlah(target: BlockVector3, mask: Mask): Pair<CuboidRegion, Int> {
        var min = target
        var max = target
        // projections: p plus, m minus
        fun px(i: Int, j: Int) = BlockVector3.at(max.x, i, j)
        fun mx(i: Int, j: Int) = BlockVector3.at(min.x, i, j)
        fun py(i: Int, j: Int) = BlockVector3.at(i, max.y, j)
        fun my(i: Int, j: Int) = BlockVector3.at(i, min.y, j)
        fun pz(i: Int, j: Int) = BlockVector3.at(i, j, max.z)
        fun mz(i: Int, j: Int) = BlockVector3.at(i, j, min.z)
        // bounds
        fun ibx() = min.y to max.y
        fun jbx() = min.z to max.z
        fun iby() = min.x to max.x
        fun jby() = min.z to max.z
        fun ibz() = min.x to max.x
        fun jbz() = min.y to max.y
        // some invariants
        // - every block inside the region has a component assigned to it
        val components = UnionFind()
        val rootCompo = components.new()
        val initialCompoMap = mapOf(target to rootCompo)
        val compoFrontiers = Array(6) { initialCompoMap }
        val projections = arrayOf(::px, ::mx, ::py, ::my, ::pz, ::mz)
        val iBounds = arrayOf(::ibx, ::ibx, ::iby, ::iby, ::ibz, ::ibz)
        val jBounds = arrayOf(::jbx, ::jbx, ::jby, ::jby, ::jbz, ::jbz)
        val directions = arrayOf(BlockVector3.UNIT_X, BlockVector3.UNIT_MINUS_X, BlockVector3.UNIT_Y, BlockVector3.UNIT_MINUS_Y, BlockVector3.UNIT_Z, BlockVector3.UNIT_MINUS_Z)
        val shouldExpand = directions.map { mask.test(target.add(it)) }.toBooleanArray()
        // IDK
        fun maybeMerge(a: Int?, b: Int?) = a?.let { _ -> b?.let { components.union(a, b) } }
        fun compoMerge(a: Int?, b: Int?, c: Int?) = maybeMerge(a, b)?.let { maybeMerge(it, c) } ?: components.new()
        fun process2(face: Int, edge: Int) {
            // face is what we're expanding, edge is what we're updating here
            val newFrontier = BlockMap.create<Int>()
            var b = false
//            for (v in this) {
//                if (!mask.test(v)) continue
//                // look backwards (and otherwards) to see existing component, merge if necessary, otherwise new compo
//                val faceCompo = compoFrontiers[face][v]
//                val otherCompo = compoFrontiers[edge][v]
//                val compo = when {
//                    faceCompo == null && otherCompo == null -> components.new()
//                    faceCompo != null && otherCompo == null -> faceCompo
//                    faceCompo == null && otherCompo != null -> otherCompo
//                    faceCompo != null && otherCompo != null -> components.union(faceCompo, otherCompo)
//                    else -> throw Exception("BUG: Impossible!")
//                }
//                newFrontier[v] = compo
//                if (compo == rootCompo) b = true
//            }
            compoFrontiers[face] = newFrontier
            shouldExpand[face] = b
        }
        fun process1(face: Int) {
            // reminder: this runs *after* expanding the face
            val newFrontier = BlockMap.create<Int>()
            val frontier = compoFrontiers[face]
            val proj = projections[face]
            val dir = directions[face]
            val (imin, imax) = iBounds[face]()
            val (jmin, jmax) = jBounds[face]()
            var b = false
            for (i in imin..imax) {
                for (j in jmin..jmax) {
                    val oldPos = proj(i, j)
                    val newPos = oldPos.add(dir)
                    if (!mask.test(newPos)) continue
                    val backCompo = frontier[oldPos]
                    val side1Compo = frontier[proj(i - 1, j).add(dir)]
                    val side2Compo = frontier[proj(i, j - 1).add(dir)]
                    val thisCompo = compoMerge(backCompo, side1Compo, side2Compo)
                    newFrontier[newPos] = thisCompo
                    // connected, should expand
                    if (thisCompo == rootCompo) b = true
                }
            }
            shouldExpand[face] = b
        }
        fun checkFace(face: Int, doExpand: (BlockVector3) -> Unit, vararg edges: Int) {
            if (!shouldExpand[face]) return
            val proj = projections[face]
            val dir = directions[face]
            for (e in edges) {
                process2(face, e)
            }
            doExpand(dir)
            process1(face)
        }
        // update minus/plus (min/max)
        fun up(x: BlockVector3) { max = max.add(x) }
        fun um(x: BlockVector3) { min = min.add(x) }
        var i = 0
        while (shouldExpand.any { it } && i < ITERATIONS_LIMIT) {
            checkFace(PX, ::up, PY, MY, PZ, MZ)
            checkFace(MX, ::um, PY, MY, PZ, MZ)
            checkFace(PY, ::up, PX, MX, PZ, MZ)
            checkFace(MY, ::um, PX, MX, PZ, MZ)
            checkFace(PZ, ::up, PX, MX, PY, MY)
            checkFace(MZ, ::um, PX, MX, PY, MY)
            i++
        }
        return CuboidRegion(min, max) to i
    }

    private fun expandRegionNew(target: BlockVector3, mask: Mask): Pair<CuboidRegion, Int> {
        val region = CuboidRegion(target, target)
        // not always the case, but here pos1 = min, pos2 = max
        fun CuboidRegion.xyMin() = CuboidRegion(pos1.withZ(pos1.z - 1), pos2.withZ(pos1.z - 1))
        fun CuboidRegion.xyMax() = CuboidRegion(pos1.withZ(pos2.z + 1), pos2.withZ(pos2.z + 1))
        fun CuboidRegion.xzMin() = CuboidRegion(pos1.withY(pos1.y - 1), pos2.withY(pos1.y - 1))
        fun CuboidRegion.xzMax() = CuboidRegion(pos1.withY(pos2.y + 1), pos2.withY(pos2.y + 1))
        fun CuboidRegion.yzMin() = CuboidRegion(pos1.withX(pos1.x - 1), pos2.withX(pos1.x - 1))
        fun CuboidRegion.yzMax() = CuboidRegion(pos1.withX(pos2.x + 1), pos2.withX(pos2.x + 1))
        val projections = arrayOf(CuboidRegion::xyMin, CuboidRegion::xyMax, CuboidRegion::xzMin, CuboidRegion::xzMax, CuboidRegion::yzMin, CuboidRegion::yzMax)
        val directions = arrayOf(BlockVector3.UNIT_MINUS_Z, BlockVector3.UNIT_Z, BlockVector3.UNIT_MINUS_Y, BlockVector3.UNIT_Y, BlockVector3.UNIT_MINUS_X, BlockVector3.UNIT_X)
        val components = UnionFind()
        val rootCompo = components.new()
        val shouldExpand = BooleanArray(6)
        val compoFrontiers = Array(6) { BlockMap.create<Int>() }
        directions.forEachIndexed { i, d ->
            val v = target.add(d)
            val b = mask.test(v)
            shouldExpand[i] = b
            if (b) compoFrontiers[i][v] = rootCompo
        }
//        val shouldExpand = projections.map { it(region).hasStuff() }.toBooleanArray()
//        val compoFrontiers = Array(6) { BlockMap.create<Int>().apply { put(target, rootCompo) } }
        fun Region.process(face: Int): Pair<BlockMap<Int>, Boolean> {
            var b = false
            val frontier = compoFrontiers[face]
            val newFrontier = BlockMap.create<Int>()
            for (v in this) {
                if (!mask.test(v)) continue
                // connectedness
                val asdf = directions.mapNotNull {
                    val vv = v.add(it)
                    frontier[vv] ?: newFrontier[vv]
                }
                val compo = if (asdf.isEmpty()) components.new() else asdf.reduce(components::union)
                newFrontier[v] = compo
                if (compo == rootCompo) b = true
            }
            return newFrontier to b
        }
        fun checkFace(i: Int, doExpand: (BlockVector3) -> Unit, vararg edges: Int) {
            if (!shouldExpand[i]) return
            val proj = projections[i]
            val dir = directions[i]
            for (e in edges) {
                val (f, b) = proj(projections[e](region)).process(e)
                if (b) {
                    shouldExpand[e] = true
                }
                compoFrontiers[e].putAll(f)
            }
            doExpand(dir)
            val (f, b) = proj(region).process(i)
            compoFrontiers[i] = f
            shouldExpand[i] = b
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
    private fun expandRegionOld(target: BlockVector3, mask: Mask): Pair<CuboidRegion, Int> {
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

private const val path_compress = true

class UnionFind {
    private val parent = arrayListOf<Int>()
    private val size = arrayListOf<Int>()
    fun new(): Int {
        val x = parent.size
        parent.add(x)
        size.add(1)
        return x
    }
    fun union(a: Int, b: Int): Int {
        var ar = find(a)
        var br = find(b)
        if (size[ar] > size[br]) {
            val tmp = ar
            ar = br
            br = tmp
        }
        // size[ar] <= size[br]
        size[br] += size[ar]
        parent[ar] = br
        return br
    }
    fun find(a: Int): Int {
        var root = a
        while (true) {
            val y = parent[root]
            if (y == root) break
            root = y
        }
        if (path_compress) {
            var y = a
            while (y != root) {
                val yp = parent[y]
                parent[y] = root
                y = yp
            }
        }
        return root
    }
}
