package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import com.sk89q.worldedit.util.formatting.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.lang.System.nanoTime
import java.util.BitSet
import java.util.HashMap
import java.util.concurrent.CompletableFuture

private const val ITERATIONS_LIMIT = 160
private const val SIZE_LIMIT = 200

@CommandAlias("/that|/hsel")
@Description("Select the build you're looking at")
@CommandPermission("redstonetools.that")
class That(private val worldEdit: WorldEdit, private val plugin: Plugin) : BaseCommand() {
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

        expandRegion(target, mask).thenAccept { (region, result) ->
            when (result) {
                is ExpandResult.Done -> {
                    val sel = CuboidRegionSelector(player.world, region.pos1, region.pos2)
                    val session = worldEdit.sessionManager.get(player)
                    session.setRegionSelector(player.world, sel)
                    sel.explainRegionAdjust(player, session)
                    player.printInfo(TextComponent.of("Build selected."))
                }
                is ExpandResult.LimitReached ->
                    player.printError(TextComponent.of("WARNING: ${result.kind} limit reached, check your selection!"))
            }
        }
    }
    sealed interface ExpandResult {
        data class LimitReached(val kind: String) : ExpandResult
        data object Done : ExpandResult
    }
    private fun expandRegion(target: BlockVector3, mask: Mask): CompletableFuture<Pair<CuboidRegion, ExpandResult>> {
        val offsets = arrayOf(
            BlockVector3.UNIT_X,
            BlockVector3.UNIT_Y,
            BlockVector3.UNIT_Z,
            BlockVector3.UNIT_MINUS_X,
            BlockVector3.UNIT_MINUS_Y,
            BlockVector3.UNIT_MINUS_Z,
        )
        var min = target
        var max = target
        val visited = BlockSet()
        val queue = ArrayDeque<BlockVector3>()
        queue.add(target)
        visited.add(target)
        fun doWork(iterations: Int) {
            var i = 0
            while (queue.isNotEmpty() && i < iterations) {
                i++
                val pos = queue.removeFirst()
                min = min.getMinimum(pos)
                max = max.getMaximum(pos)
                for (it in offsets) {
                    val newPos = pos.add(it)
                    if (newPos in visited) continue
                    visited.add(newPos)
                    if (mask.test(newPos)) {
                        queue.addLast(newPos)
                    }
                }
            }
        }
        val future = CompletableFuture<Pair<CuboidRegion, ExpandResult>>()
        var nextIters = 0
        fun next() {
            nextIters++
            val startNs = nanoTime()
            // TODO extract config options
            var sizeLimitReached = false
            while (nanoTime() - startNs < 30_000_000 && !sizeLimitReached && queue.isNotEmpty()) {
                doWork(1000)
                sizeLimitReached = max.subtract(min).run { x >= SIZE_LIMIT || y >= SIZE_LIMIT || z >= SIZE_LIMIT }
            }
            val res = when {
                queue.isEmpty() ->  ExpandResult.Done
                sizeLimitReached -> ExpandResult.LimitReached("size")
                nextIters >= 5 -> ExpandResult.LimitReached("time")
                else -> {
                    Bukkit.getScheduler().runTaskLater(plugin, ::next, 1)
                    return
                }
            }
            future.complete(CuboidRegion(min, max) to res)
        }
        next()
        return future
    }

}

private const val X_BITS = 4
private const val Y_BITS = 4
private const val Z_BITS = 4
private const val CHUNK_SIZE = 1 shl (X_BITS + Y_BITS + Z_BITS)

class BlockSet {
    private val map = HashMap<Long, BitSet>()
    private fun bitSet(v: BlockVector3): BitSet {
        val yRestBits = 9 - Y_BITS
        val x = (v.x ushr X_BITS).toLong() shl (32 - Z_BITS + yRestBits)
        val z = (v.z ushr Z_BITS).toLong() shl yRestBits
        val y = ((v.y ushr Y_BITS) and ((1 shl yRestBits) - 1)).toLong()
        val key = x or z or y
        return map.getOrPut(key) { BitSet(CHUNK_SIZE) }
    }
    private fun bit(v: BlockVector3): Int {
        val x = v.x and ((1 shl X_BITS) - 1)
        val y = (v.y and ((1 shl Y_BITS) - 1)) shl X_BITS
        val z = (v.z and ((1 shl Z_BITS) - 1)) shl (X_BITS + Y_BITS)
        return x or y or z
    }
    fun add(v: BlockVector3) {
        bitSet(v).set(bit(v))
    }
    operator fun contains(v: BlockVector3): Boolean = bitSet(v).get(bit(v))
}
