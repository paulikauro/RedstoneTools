package redstonetools

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.platform.PlayerInputEvent
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.eventbus.Subscribe
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.extra.kotlin.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration.separator
import net.kyori.adventure.text.format.NamedTextColor.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Criteria
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
        val selection = session.getSelectionOrNull() ?: run {
            player.hideHelper()
            return
        }
        val pos1 = selection.boundingBox.pos1
        val pos2 = selection.boundingBox.pos2
        fun BlockVector3.format() = arrayOf(blockX, blockY, blockZ).map { "$it"[GRAY] }
            .join(separator(","[GRAY]))

        val lines = buildList {
            add("Position 1:"[DARK_GREEN])
            add(text("   ") + pos1.format())
            val volume = selection.volume
            if (volume != 1L) {
                add("Position 2:"[DARK_GREEN])
                add(text("   ") + pos2.format())
            }
            add("Volume:"[DARK_GREEN])
            val volColor = when {
                volume < 100000 -> GREEN
                volume < 1000000 -> YELLOW
                volume < 2000000 -> RED
                else -> DARK_RED
            }
            add("   $volume"[volColor])
            add("Dimensions:"[DARK_GREEN])
            fun dimColor(x: Int) = when {
                x < 50 -> GREEN
                x < 75 -> YELLOW
                x < 100 -> RED
                else -> DARK_RED
            }

            val line = with(selection) { arrayOf(width, height, length) }
                .map { "$it"[dimColor(it)] }
                .join(separator("x"[GRAY]))
            add(text("   ") + line)
        }
        player.scoreboard = Bukkit.getScoreboardManager().newScoreboard.apply {
            registerNewObjective(
                Random.nextInt(1234567890).toString(),
                Criteria.DUMMY,
                "Current selection"[RED],
            ).apply {
                displaySlot = DisplaySlot.SIDEBAR
                addLinesToScoreboard(lines)
            }
        }
    }

    private fun Player.hideHelper() {
        scoreboard = Bukkit.getScoreboardManager().newScoreboard
    }

    private fun Objective.addLinesToScoreboard(lines: List<Component>) {
        lines.reversed().forEachIndexed { index, line ->
            getScore("wehelper-$index").apply {
                score = index + 1
                customName(line)
            }
        }
    }
}
