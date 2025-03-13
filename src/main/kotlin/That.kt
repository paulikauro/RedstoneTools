package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.LocalSession
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


data class ThatConfig(
    val sizeLimit: Int = 200,
    val maxTimePerTickMs: Int = 30,
    val maxTicks: Int = 5,
)

// unsure if this needs to be configurable, but probably doesn't matter
private const val ITERATIONS_PER_BURST = 2000

@CommandAlias("/that|/hsel")
@Description("Select the build you're looking at")
@CommandPermission("redstonetools.that")
class That(private val config: ThatConfig, private val worldEdit: WorldEdit, private val plugin: Plugin) : BaseCommand() {
    private val sizeLimit = config.sizeLimit
    private val maxNsPerTick = config.maxTimePerTickMs * 1_000_000

    @Default
    @CommandCompletion("@we_mask")
    fun that(
        player: WEPlayer,
        localSession: LocalSession,
        args: Array<String>,
    ) {
        // very crappy argument parsing
        // plan is to replace ACF at some point so not going to waste a lot of effort in this
        var offsets = Offsets.DEFAULT
        var maskStr = "#existing"
        for (arg in args) {
            when (arg) {
                "-d" -> offsets = Offsets.DIAG
                "-dd" -> offsets = Offsets.VERY_DIAG
                else -> maskStr = arg
            }
        }
        val mask = parseMaskOrThrow(maskStr, worldEdit, localSession, player)
        // NOTE: this does not use the mask!
        val target = player.getBlockTrace(config.sizeLimit)?.toVector()?.toBlockPoint() ?: run {
            player.printError(TextComponent.of("No build in sight!"))
            return
        }

        expandRegion(target, mask, offsets).thenAccept { (region, result) ->
            when (result) {
                is ExpandResult.Done -> {
                    val sel = CuboidRegionSelector(player.world, region.pos1, region.pos2)
                    val session = worldEdit.sessionManager.get(player)
                    session.setRegionSelector(player.world, sel)
                    sel.explainRegionAdjust(player, session)
                    player.printInfo(TextComponent.of("Build selected."))
                }
                is ExpandResult.LimitExceeded ->
                    player.printError(TextComponent.of( "${result.kind} limit exceeded. Your selection was not changed."))
            }
        }
    }

    sealed interface ExpandResult {
        data class LimitExceeded(val kind: String) : ExpandResult
        data object Done : ExpandResult
    }

    private fun expandRegion(target: BlockVector3, mask: Mask, offsets: List<BlockVector3>): CompletableFuture<Pair<CuboidRegion, ExpandResult>> {
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
        var ticks = 0
        fun next() {
            ticks++
            val startNs = nanoTime()
            var sizeLimitReached = false
            while (nanoTime() - startNs <= maxNsPerTick && !sizeLimitReached && queue.isNotEmpty()) {
                doWork(ITERATIONS_PER_BURST)
                sizeLimitReached = max.subtract(min).run { x > sizeLimit || y > sizeLimit || z > sizeLimit }
            }
            val res = when {
                queue.isEmpty() ->  ExpandResult.Done
                sizeLimitReached -> ExpandResult.LimitExceeded("Size")
                ticks > config.maxTicks -> ExpandResult.LimitExceeded("Time")
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

private class BlockSet {
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

private object Offsets {
    private fun v(x: Int, y: Int, z: Int) = BlockVector3.at(x, y, z)

    val DEFAULT = listOf(
        v(1, 0, 0),
        v(-1, 0, 0),
        v(0, 1, 0),
        v(0, -1, 0),
        v(0, 0, 1),
        v(0, 0, -1),
    )
    val DIAG = DEFAULT + listOf(
        // top layer
        v(1, 1, 0),
        v(-1, 1, 0),
        v(0, 1, 1),
        v(0, 1, -1),

        // bottom layer
        v(1, -1, 0),
        v(-1, -1, 0),
        v(0, -1, 1),
        v(0, -1, -1),

        // mid layer
        v(1, 0, 1),
        v(-1, 0, 1),
        v(1, 0, -1),
        v(-1, 0, -1),
    )
    val VERY_DIAG = DIAG + listOf(
        // top
        v(1, 1, 1),
        v(-1, 1, 1),
        v(1, 1, -1),
        v(-1, 1, -1),
        // bottom
        v(1, -1, 1),
        v(-1, -1, 1),
        v(1, -1, -1),
        v(-1, -1, -1),
    )
}
