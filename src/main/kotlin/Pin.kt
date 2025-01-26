package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.type.Switch
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.*

@CommandAlias("pin")
@Description("Pin your favorite redstones")
@CommandPermission("redstonetools.pin")
class PinCommand(private val plugin: Plugin) : BaseCommand() {
    // Data class to represent a pin
    data class Pin(val location: Location) {
        private enum class PinStateResult { OK, DESTROYED }

        // Modify the lever's state using a provided modifier function
        fun modifyState(modifier: (Boolean) -> Boolean): PinStateResult {
            val block = location.block
            val lever = block.blockData as? Switch ?: return PinStateResult.DESTROYED

            // Modify the powered state
            lever.isPowered = modifier(lever.isPowered)
            block.blockData = lever

            // Ensure the block updates properly
            block.state.update(true, true)

            return PinStateResult.OK
        }

        // Set the lever's state directly (wrapper for modifyState)
        fun setState(newState: Boolean): PinStateResult {
            return modifyState { _ -> newState }
        }
    }

    private val pins = mutableMapOf<Pair<UUID, String>, Pin>()
    private val blockListener = BlockListener()
    val listener: Listener get() = blockListener

    inner class CompletionHandler :
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
        override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
            val player = context.sender as Player
            return pins.keys.filter { (uuid, _) -> uuid == player.uniqueId }.map { (_, name) -> name }
        }
    }

    @Default
    @CatchUnknown
    fun help(player: Player) {
        player.sendMessage("Unknown subcommand! Use tab completion or refer to #announcements message")
    }

    @Subcommand("list")
    @Description("List your pins")
    @CommandPermission("redstonetools.pin.list")
    fun list(player: Player) {
        player.sendMessage("Your pins:")
        pins
            .filterKeys { (uuid, _) -> uuid == player.uniqueId }
            .map { (key, value) -> "${key.second} at ${value.location.toBlockVector3()}" }
            .forEach(player::sendMessage)
    }

    @Subcommand("add")
    @Description("Add a pin")
    @CommandPermission("redstonetools.pin.add")
    fun add(player: Player, name: String) {
        if (player.uniqueId to name in pins) {
            player.sendMessage("Pin $name already exists!")
            return
        }

        val result = blockListener.add(player) { event ->
            if (event.block.type != Material.LEVER) {
                player.sendMessage("That's not a lever! Restart by doing /pin add $name")
                return@add
            }
            pins[player.uniqueId to name] = Pin(event.block.location)
            player.sendMessage("Pin $name added")
        }

        when (result) {
            BlockListener.BlockResult.ADDED -> player.sendMessage("Break the lever you want added as a pin")
            BlockListener.BlockResult.EXISTS -> player.sendMessage("You're already adding a pin")
        }
    }

    @Subcommand("remove")
    @Description("Remove a pin")
    @CommandPermission("redstonetools.pin.remove")
    @CommandCompletion("@pins")
    fun remove(player: Player, name: String) {
        val removed = pins.remove(player.uniqueId to name) != null
        val message = if (removed) "Pin $name removed" else "No pin named $name"
        player.sendMessage(message)
    }

    @Subcommand("turn")
    @Description("Change pin state")
    @CommandPermission("redstonetools.pin.turn")
    @CommandCompletion("@pins")
    fun turn(player: Player, newState: Boolean, name: String) {
        val pin = pins[player.uniqueId to name] ?: run {
            player.sendMessage("No pin named $name")
            return
        }
        when (pin.setState(newState)) {
            Pin.PinStateResult.OK -> player.sendMessage("Turned $name ${if (newState) "ON" else "OFF"}")
            Pin.PinStateResult.DESTROYED -> player.sendMessage("Pin $name has been destroyed!")
        }
    }

    @Subcommand("toggle")
    @Description("Toggle the state of a pin")
    @CommandPermission("redstonetools.pin.toggle")
    @CommandCompletion("@pins")
    fun toggle(player: Player, name: String) {
        val pin = pins[player.uniqueId to name] ?: run {
            player.sendMessage("No pin named $name")
            return
        }

        val result = pin.modifyState(Boolean::not) // Toggle the state
        when (result) {
            Pin.PinStateResult.OK -> player.sendMessage("Pin $name toggled!")
            Pin.PinStateResult.DESTROYED -> player.sendMessage("Pin $name has been destroyed!")
        }
    }

    @Subcommand("pulse")
    @Description("Pulse a pin")
    @CommandPermission("redstonetools.pin.pulse")
    @CommandCompletion("@pins")
    fun pulse(player: Player, name: String, time: Int) {
        if (time < 1 || time > 100) {
            player.sendMessage("Time must be between 1 and 100 ticks!")
            return
        }

        val pin = pins[player.uniqueId to name] ?: run {
            player.sendMessage("No pin named $name")
            return
        }

        when (pin.setState(true)) {
            Pin.PinStateResult.OK -> {
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    pin.setState(false)
                }, time.toLong())
            }
            Pin.PinStateResult.DESTROYED -> player.sendMessage("Pin $name has been destroyed!")
        }
    }

    private class BlockListener : Listener {
        private val players = mutableMapOf<UUID, (BlockBreakEvent) -> Unit>()

        enum class BlockResult { ADDED, EXISTS }

        fun add(player: Player, onBreak: (BlockBreakEvent) -> Unit): BlockResult {
            if (player.uniqueId in players) return BlockResult.EXISTS
            players[player.uniqueId] = onBreak
            return BlockResult.ADDED
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        fun onBlockBreak(event: BlockBreakEvent) {
            val handler = players.remove(event.player.uniqueId) ?: return
            event.isCancelled = true
            handler(event)
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            players.remove(event.player.uniqueId)
        }

        @EventHandler
        fun onPlayerKick(event: PlayerKickEvent) {
            players.remove(event.player.uniqueId)
        }
    }
}
