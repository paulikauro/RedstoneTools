package redstonetools

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.platform.PlayerInputEvent
import com.sk89q.worldedit.util.eventbus.Subscribe
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.ChatColor.*
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import kotlin.random.Random

class WorldEditHelper(plugin: JavaPlugin, private val worldEdit: WorldEdit) : Listener {

    init {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkPlayers, 0, 20)
        worldEdit.eventBus.register(this)
    }

    @Subscribe
    fun updateSelection(event: PlayerInputEvent) {
        val actor = event.player
        if (actor != null && actor.isPlayer) {
            setPlayerSelection(BukkitAdapter.adapt(actor))
        }
    }

    private fun checkPlayers() {
        Bukkit.getOnlinePlayers().forEach(this::setPlayerSelection)
    }

    private fun setPlayerSelection(player: Player) {
        val session = worldEdit.sessionManager.get(BukkitAdapter.adapt(player))
        val selection = try {
            session.getSelection(session.selectionWorld ?: throw IncompleteRegionException())
        } catch (exception: IncompleteRegionException) {
            player.hideHelper()
            return
        }
        val bvMax = selection.maximumPoint
        val bvMin = selection.minimumPoint
        val lines = buildList {
            add("${DARK_GREEN}Position A:")
            add("   $GRAY${bvMax.blockX}$WHITE,$GRAY${bvMax.blockY}$WHITE,$GRAY${bvMax.blockZ}")
            val volume = selection.volume
            if (volume != 1L) {
                add("${DARK_GREEN}Position B:")
                add("   $GRAY${bvMin.blockX}$WHITE,$GRAY${bvMin.blockY}$WHITE,$GRAY${bvMin.blockZ}")
            }
            add("${DARK_GREEN}Volume:")
            val chatColor = when {
                volume < 100000 -> GREEN
                volume < 1000000 -> YELLOW
                volume < 2000000 -> RED
                else -> DARK_RED
            }
            add("   $chatColor$volume")
            add("${DARK_GREEN}Dimensions:")
            val dimensionsX = bvMax.blockX - bvMin.blockX + 1
            val dimensionsY = bvMax.blockY - bvMin.blockY + 1
            val dimensionsZ = bvMax.blockZ - bvMin.blockZ + 1
            fun getBlockColors(dimensionsX: Int, dimensionsY: Int, dimensionsZ: Int): List<String> {
                val colorRanges = listOf(50 to GREEN, 75 to YELLOW, 100 to RED)
                fun getColor(dim: Int): ChatColor {
                    return colorRanges.firstOrNull { dim < it.first }?.second ?: DARK_RED
                }
                return listOf(dimensionsX, dimensionsY, dimensionsZ).map { dim -> "${getColor(dim)}$dim" }
            }
            val results = getBlockColors(dimensionsX, dimensionsY, dimensionsZ)
            add("   ${results[0]}$WHITEx${results[1]}$WHITEx${results[2]}")
        }
        player.scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard.apply {
            registerNewObjective(
                Random.nextInt(1234567890).toString(),
                "dummy",
                "Current selection"
            ).apply {
                displaySlot = DisplaySlot.SIDEBAR
                displayName = "${RED}Current Selection"
                addLinesToScoreboard(lines)
            }
        }
    }

    private fun Player.hideHelper() {
        scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
    }

    private fun Objective.addLinesToScoreboard(lines: List<String>) {
        lines.reversed().forEachIndexed { index, line ->
            getScore(line).score = index + 1
        }
    }
}
