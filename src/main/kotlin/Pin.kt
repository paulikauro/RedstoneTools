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
    // currently only input pin
    data class Pin(val location: Location)
    private val pins = mutableMapOf<Pair<UUID, String>, Pin>()

    private enum class PinStateResult { OK, DESTROYED }
    private fun Pin.setState(
        newState: PinState,
    ): PinStateResult {
        val block = location.block
        val lever = block.blockData as? Switch ?: return PinStateResult.DESTROYED
        lever.isPowered = newState.value
        block.blockData = lever
        return PinStateResult.OK
    }

    private val blockListener = BlockListener()
    val listener: Listener get() = blockListener

    inner class CompletionHandler :
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
        override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
            val player = context.sender as Player
            return pins.keys.filter { (uuid, _) -> uuid == player.uniqueId }.map { (_, name) -> name }
        }
    }

    @Subcommand("add")
    @Description("Add a pin")
    @CommandPermission("redstonetools.pin.add")
    fun add(player: Player, name: String) {
        if (player.uniqueId to name in pins) {
            player.sendMessage("Pin $name already exists!")
            return
        }
        // this control flow is too backwards
        val result = blockListener.add(player) { event ->
            if (event.block.type != Material.LEVER) {
                // this should just ask you to try again
                player.sendMessage("That's not a lever! Restart by doing /pin add $name")
                return@add
            }
            pins[player.uniqueId to name] = Pin(event.block.location)
            player.sendMessage("Pin $name added")
        }
        when (result) {
            // :(
            BlockListener.BlockResult.ADDED -> "Break the lever you want added as a pin"
            BlockListener.BlockResult.EXISTS -> "You're already adding a pin"
        }.let(player::sendMessage)
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
    @CommandCompletion("@pin_state @pins")
    fun turn(player: Player, newState: PinState, name: String) {
        val pin = pins[player.uniqueId to name] ?: run {
            player.sendMessage("No pin named $name")
            return
        }
        when (pin.setState(newState)) {
            PinStateResult.OK -> "Turned $name $newState"
            PinStateResult.DESTROYED -> "Pin $name has been destroyed!"
        }.let(player::sendMessage)
    }

    @Subcommand("pulse")
    @Description("Pulse a pin")
    @CommandPermission("redstonetools.pin.pulse")
    @CommandCompletion("@pin_state @pins @range:1-100")
    fun pulse(player: Player, state: PinState, name: String, time: Int) {
        val pin = pins[player.uniqueId to name] ?: run {
            player.sendMessage("No pin named $name")
            return
        }
        when (pin.setState(state)) {
            PinStateResult.OK -> {}
            PinStateResult.DESTROYED -> {
                player.sendMessage("Pin $name has been destroyed!")
                return
            }
        }
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            // todo deal with error
            pin.setState(PinState(!state.value))
        }, time * 2L)
    }
}

private typealias BlockHandler = (BlockBreakEvent) -> Unit
private class BlockListener : Listener {
    private val players = mutableMapOf<UUID, BlockHandler>()

    enum class BlockResult { ADDED, EXISTS }

    fun add(player: Player, onXd: BlockHandler): BlockResult {
        if (player.uniqueId in players) return BlockResult.EXISTS
        players[player.uniqueId] = onXd
        return BlockResult.ADDED
    }

    @EventHandler
    fun onLeaveEvent(event: PlayerQuitEvent) {
        players.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onKickEvent(event: PlayerKickEvent) {
        players.remove(event.player.uniqueId)
    }

    // TODO: permission check? is it needed elsewhere?
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val handler = players.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        handler(event)
    }
}
