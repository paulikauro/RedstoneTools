package io.github.paulikauro.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.type.Switch
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.*

fun PluginScope.createPins() {
    val pins = PinCommand(plugin)
    commandManager.apply {
        commandCompletions.registerCompletion(COMPLETION_PINS, pins.CompletionHandler())
        registerThing(PinState)
        registerCommand(pins)
    }
    pluginManager.registerEvents(pins.listener, plugin)
}

private const val COMPLETION_PINS = "pins"

@CommandAlias("pin")
@Description("Pin your favorite redstones")
@CommandPermission("redstonetools.pin")
private class PinCommand(private val plugin: Plugin) : BaseCommand() {
    // currently only input pin
    data class Pin(val location: Location)

    private val pins = mutableMapOf<Pair<UUID, String>, Pin>()

    private fun Pin.setState(player: Player, newState: PinState) {
        modifyState(player) { newState }
    }

    private val PIN_DESTROYED = RedstoneToolsException("No lever at pin location!")
    private val NO_ACCESS = RedstoneToolsException("No access to pin location!")
    private fun Pin.modifyState(player: Player, f: (PinState) -> PinState): PinState {
        val block = location.block
        val lever = block.blockData as? Switch ?: throw PIN_DESTROYED
        val newState = f(PinState(lever.isPowered))
        val level = (location.world as CraftWorld).handle
        val pos = (block as CraftBlock).position
        val nmsPlayer = (player as CraftPlayer).handle
        if (lever.isPowered != newState.value) {
            // spawn protection & world border
            if (!level.mayInteract(nmsPlayer, pos)) {
                throw NO_ACCESS
            }
            nmsPlayer.gameMode.useItemOn(
                nmsPlayer, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                BlockHitResult(
                    Vec3.atCenterOf(pos),
                    Direction.DOWN, pos, true,
                ),
            )
        }
        return newState
    }

    val listener: Listener
        field = BlockListener()

    inner class CompletionHandler :
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
        override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> {
            val player = context.sender as Player
            return pins.keys.filter { (uuid, _) -> uuid == player.uniqueId }.map { (_, name) -> name }
        }
    }

    @HelpCommand
    fun help(help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("list")
    @Description("List your pins")
    @CommandPermission("redstonetools.pin.list")
    fun list(player: Player) {
        player.info("Your pins:")
        pins
            .filterKeys { (uuid, _) -> uuid == player.uniqueId }
            // TODO: click to tp
            .map { (key, value) -> "${key.second} at ${value.location.toBlockVector3()}" }
            .forEach(player::info)
    }

    @Subcommand("add")
    @Description("Add a pin")
    @CommandPermission("redstonetools.pin.add")
    fun add(player: Player, name: String) {
        if (player.uniqueId to name in pins) {
            player.info("Pin $name already exists!")
            return
        }
        // this control flow is too backwards
        val result = listener.add(player) { event ->
            if (event.block.type != Material.LEVER) {
                // this should just ask you to try again
                player.info("That's not a lever! Restart by doing /pin add $name")
                return@add
            }
            pins[player.uniqueId to name] = Pin(event.block.location)
            player.info("Pin $name added")
        }
        when (result) {
            // :(
            BlockListener.BlockResult.ADDED -> "Break the lever you want added as a pin"
            BlockListener.BlockResult.EXISTS -> "You're already adding a pin"
        }.let(player::info)
    }

    @Subcommand("remove")
    @Description("Remove a pin")
    @CommandPermission("redstonetools.pin.remove")
    @CommandCompletion("@$COMPLETION_PINS")
    fun remove(player: Player, name: String) {
        val removed = pins.remove(player.uniqueId to name) != null
        val message = if (removed) "Pin $name removed" else "No pin named $name"
        player.info(message)
    }

    @Subcommand("turn")
    @Description("Change pin state")
    @CommandPermission("redstonetools.pin.turn")
    @CommandCompletion("@pin_state @$COMPLETION_PINS")
    fun turn(player: Player, newState: PinState, name: String) {
        val pin = pins[player.uniqueId to name] ?: run {
            player.info("No pin named $name")
            return
        }
        pin.setState(player, newState)
        player.info("Turned $name $newState")
    }

    @Subcommand("pulse")
    @Description("Pulse a pin")
    @CommandPermission("redstonetools.pin.pulse")
    @CommandCompletion("@pin_state @$COMPLETION_PINS @range:1-100")
    fun pulse(player: Player, state: PinState, name: String, time: Int) {
        if (time !in 1..100) {
            player.info("Time must be between 1 and 100 ticks (inclusive)!")
            return
        }
        val pin = pins[player.uniqueId to name] ?: run {
            player.info("No pin named $name")
            return
        }
        pin.setState(player, state)
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            // refresh player object
            // not ideal to just return if player not found, but it's the safest from a permissions point of view
            val player = plugin.server.getPlayer(player.uniqueId) ?: return@Runnable
            try {
                pin.setState(player, state.not())
            } catch (e: RedstoneToolsException) {
                player.err(e.message)
            }
        }, time * 2L)
    }

    @Subcommand("toggle")
    @Description("Toggle pin state")
    @CommandPermission("redstonetools.pin.toggle")
    @CommandCompletion("@$COMPLETION_PINS")
    fun toggle(player: Player, name: String) {
        val pin = pins[player.uniqueId to name] ?: run {
            player.info("No pin named $name")
            return
        }

        val newState = pin.modifyState(player, PinState::not)
        player.info("Toggled $name to $newState")
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
    // it checks for cancellation now to address that ^
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val handler = players.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        handler(event)
    }
}

private class PinState(val value: Boolean) {
    override fun toString(): String = when (value) {
        false -> "off"
        true -> "on"
    }

    fun not(): PinState = PinState(!value)

    companion object : Thing<PinState> {
        override val readableName = "Pin state"

        override fun of(arg: String): PinState? = when (arg) {
            "on" -> PinState(true)
            "off" -> PinState(false)
            else -> null
        }

        override val values = listOf("on", "off")
        override val valueClass = PinState::class.java
    }
}
