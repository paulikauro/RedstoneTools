package io.github.paulikauro.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.lang.System.nanoTime
import java.util.BitSet
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayDeque

data class ThatConfig(
    val sizeLimit: Int = 200,
    val maxTimePerTickMs: Int = 30,
    val maxTicks: Int = 5,
)

fun PluginScope.createThat(config: ThatConfig, worldEdit: WorldEdit) {
    commandManager.registerCommand(That(config, worldEdit, plugin))
}

// unsure if this needs to be configurable, but probably doesn't matter
private const val ITERATIONS_PER_BURST = 2000

@CommandAlias("/that|/hsel")
@Description("Select the build you're looking at")
@CommandPermission("redstonetools.that")
private class That(private val config: ThatConfig, private val worldEdit: WorldEdit, private val plugin: Plugin) :
    BaseCommand() {
    private val sizeLimit = config.sizeLimit
    private val maxNsPerTick = config.maxTimePerTickMs * 1_000_000L

    @Default
    @CommandCompletion("@$COMPLETION_MASK")
    fun that(
        player: WEPlayer,
        localSession: LocalSession,
        args: Array<String>,
    ) {
        // very crappy argument parsing
        // the plan is to replace ACF at some point so not going to waste a lot of effort on this
        var offsets = Offsets.DEFAULT
        var maskStr = "#existing"
        var inQuotes = false
        for (arg in args) {
            if (inQuotes) {
                inQuotes = !arg.endsWith("\"")
                maskStr += " $arg".removeSuffix("\"")
                continue
            }
            if (arg.startsWith("\"")) {
                maskStr = arg.removePrefix("\"").removeSuffix("\"")
                inQuotes = !arg.endsWith("\"")
                continue
            }
            when (arg) {
                "-d" -> offsets = Offsets.DIAG
                "-dd" -> offsets = Offsets.VERY_DIAG
                "-ddd" -> offsets = Offsets.VERY_VERY_DIAG
                else -> maskStr = arg
            }
        }
        if (inQuotes) throw RedstoneToolsException("Unterminated quote")
        val mask = parseMaskOrThrow(maskStr, worldEdit, localSession, player)
        val target = player.getBlockTrace(config.sizeLimit, false, mask)?.toVector()?.toBlockPoint()
            ?: return player.err("No build in sight!")

        expandRegion(target, mask, offsets).thenAccept { (region, result) ->
            when (result) {
                is ExpandResult.Done -> {
                    val sel = CuboidRegionSelector(player.world, region.pos1, region.pos2)
                    val session = worldEdit.sessionManager.get(player)
                    session.setRegionSelector(player.world, sel)
                    sel.explainRegionAdjust(player, session)
                    player.info("Build selected.")
                }

                is ExpandResult.LimitExceeded ->
                    player.err("${result.kind} limit exceeded. Your selection was not changed.")
            }
        }
    }

    sealed interface ExpandResult {
        data class LimitExceeded(val kind: String) : ExpandResult
        data object Done : ExpandResult
    }

    private fun expandRegion(
        target: BlockVector3,
        mask: Mask,
        offsets: List<BlockVector3>,
    ): CompletableFuture<Pair<CuboidRegion, ExpandResult>> {
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
                sizeLimitReached = max.subtract(min).run { x() > sizeLimit || y() > sizeLimit || z() > sizeLimit }
            }
            val res = when {
                queue.isEmpty() -> ExpandResult.Done
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

internal class BlockSet {
    companion object {
        // See also BlockSetTests for constraints on the bit sizes
        // How many least significant bits to take from each axis for a chunk?
        internal const val X_OFFSET_BITS = 4
        internal const val Z_OFFSET_BITS = 4
        internal const val Y_OFFSET_BITS = 4

        // How many bits for the chunk id?
        internal const val X_CHUNK_BITS = 24
        internal const val Z_CHUNK_BITS = 24
        internal const val Y_CHUNK_BITS = 16

        private const val OFFSET_BITS = X_OFFSET_BITS + Z_OFFSET_BITS + Y_OFFSET_BITS

    }

    private val map = HashMap<Long, BitSet>()
    private fun bitSetOfChunk(v: BlockVector3): BitSet {
        val chunkX = v.x().bits(X_OFFSET_BITS, X_CHUNK_BITS)
        val chunkZ = v.z().bits(Z_OFFSET_BITS, Z_CHUNK_BITS) shl X_CHUNK_BITS
        val chunkY = v.y().bits(Y_OFFSET_BITS, Y_CHUNK_BITS) shl (X_CHUNK_BITS + Z_CHUNK_BITS)
        val key = chunkX or chunkZ or chunkY
        return map.getOrPut(key) { BitSet(1 shl OFFSET_BITS) }
    }

    private fun offsetInChunk(v: BlockVector3): Int {
        val x = v.x() and X_OFFSET_BITS.mask
        val z = (v.z() and Z_OFFSET_BITS.mask) shl X_OFFSET_BITS
        val y = (v.y() and Y_OFFSET_BITS.mask) shl (X_OFFSET_BITS + Z_OFFSET_BITS)
        return x or y or z
    }

    fun add(v: BlockVector3) {
        bitSetOfChunk(v).set(offsetInChunk(v))
    }

    operator fun contains(v: BlockVector3): Boolean = bitSetOfChunk(v).get(offsetInChunk(v))

    private inline val Int.mask: Int get() = (1 shl this) - 1
    private inline fun Int.bits(start: Int, len: Int): Long = ((this ushr start) and len.mask).toUInt().toLong()
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
    )
    val VERY_DIAG = DIAG + listOf(
        // mid layer
        v(1, 0, 1),
        v(-1, 0, 1),
        v(1, 0, -1),
        v(-1, 0, -1),
    )
    val VERY_VERY_DIAG = VERY_DIAG + listOf(
        // top corners
        v(1, 1, 1),
        v(-1, 1, 1),
        v(1, 1, -1),
        v(-1, 1, -1),
        // bottom corners
        v(1, -1, 1),
        v(-1, -1, 1),
        v(1, -1, -1),
        v(-1, -1, -1),
    )
}
